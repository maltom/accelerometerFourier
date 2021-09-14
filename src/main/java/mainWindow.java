import javax.swing.*;
import java.awt.*;
import java.awt.event.ActionEvent;
import java.awt.event.ActionListener;
import java.io.IOException;
import java.util.Arrays;

import com.fazecast.jSerialComm.*;
import org.jfree.chart.ChartFactory;
import org.jfree.chart.ChartPanel;
import org.jfree.chart.JFreeChart;
import org.jfree.chart.plot.PlotOrientation;
import org.jfree.chart.plot.XYPlot;
import org.jfree.chart.renderer.xy.XYLineAndShapeRenderer;
import org.jfree.data.xy.DefaultXYDataset;
import org.jfree.data.xy.XYDataset;
import org.jfree.data.xy.XYSeries;
import org.jfree.data.xy.XYSeriesCollection;
import scala.collection.JavaConverters;
import scala.collection.Seq;
import scala.concurrent.JavaConversions;

import static java.lang.Math.pow;

public class mainWindow implements ActionListener, SerialPortPacketListener
{


    JFrame frame;
    JPanel panel;
    JButton connectButton;
    JButton disconnectButton;

    JFreeChart timeChart;
    ChartPanel timeChartPanel;
    JFreeChart fourierChart;
    ChartPanel fourierChartPanel;

    XYSeriesCollection timeSeries;
    XYSeriesCollection fourierSeries;

    private String serialName = "COM3";
    private SerialPort serialRx;
    private int messageLength = 1024;
    private int maxMeasuredFreq = 50;
    private int ADCresolution = 8;
    private double ADCVolt=3.3;
    private double voltageADCFactor = ADCVolt/pow(2,ADCresolution);
    private byte [] timeDomain;


    public mainWindow() throws IOException
    {
        setupGui();
        setupListeners();
        setupSerial();
    }
    public static void main(String[] args) throws IOException
    {
        new mainWindow();
    }
    private void setupGui()
    {
        frame = new JFrame();
        panel = new JPanel();
        panel.setBorder(BorderFactory.createEmptyBorder(5,5,5,5));
        panel.setLayout(new GridLayout(2,2));

        connectButton = new JButton("Connect");
        disconnectButton = new JButton("Disconnect");
        connectButton.setSize(100,30);
        disconnectButton.setSize(100, 30);

        panel.add(connectButton);
        panel.add(disconnectButton);
        setupChart();

        frame.add(panel,BorderLayout.CENTER);
        frame.setDefaultCloseOperation((JFrame.EXIT_ON_CLOSE));
        frame.setTitle("Accelerometer Fourier Analysis");
        frame.pack();
        frame.setVisible(true);

    }

    private void setupSerial()
    {
        serialRx = SerialPort.getCommPort(serialName);
        //serialListener = new PacketListener();
        serialRx.setBaudRate(128000);
        serialRx.setNumDataBits(8);
        serialRx.setNumStopBits(1);
        serialRx.setParity(0);
    }
    private void setupChart()
    {
        timeSeries = new XYSeriesCollection();
        fourierSeries = new XYSeriesCollection();

        timeChart = ChartFactory.createXYLineChart("Time Domain","Miliseconds","Voltage",timeSeries, PlotOrientation.VERTICAL,false,false,false);
        fourierChart = ChartFactory.createXYLineChart("Frequency Domain","Hertz","Value",fourierSeries, PlotOrientation.VERTICAL,false,false,false);

        timeChartPanel = new ChartPanel(timeChart);
        fourierChartPanel = new ChartPanel(fourierChart);

        timeChartPanel.setPreferredSize( new java.awt.Dimension( 800 , 600 ) );
        fourierChartPanel.setPreferredSize( new java.awt.Dimension( 800 , 600 ) );

        final XYPlot timePlot = timeChart.getXYPlot( );
        final XYPlot fourierPlot = fourierChart.getXYPlot( );

        //timeChartPanel.setPreferredSize( new java.awt.Dimension( 800 , 600 ) );
        XYLineAndShapeRenderer timeRenderer = new XYLineAndShapeRenderer( );
        XYLineAndShapeRenderer fourierRenderer = new XYLineAndShapeRenderer( );

        timeRenderer.setSeriesPaint( 0 , Color.GREEN );
        fourierRenderer.setSeriesPaint( 0 , Color.GREEN );

        timePlot.setRenderer(timeRenderer);
        fourierPlot.setRenderer(fourierRenderer);

        panel.add(timeChartPanel);
        panel.add(fourierChartPanel);

    }

    private void setupListeners()
    {
        connectButton.addActionListener(this);
        disconnectButton.addActionListener(this);
    }

    @Override
    public void actionPerformed(ActionEvent e)
    {
        if(e.getSource()==connectButton)
        {
            if(!serialRx.isOpen()) {
                boolean success = serialRx.openPort();
                if(success) {

                    serialRx.addDataListener(this);
                    disconnectButton.setEnabled(true);
                    connectButton.setEnabled(false);
                }
            }

        }
        else if(e.getSource()==disconnectButton)
        {
            if(serialRx.isOpen()) {
                serialRx.removeDataListener();
                serialRx.closePort();
                disconnectButton.setEnabled(false);
                connectButton.setEnabled(true);
            }
        }
    }
    private void refreshTimeDataSeries(byte [] data) {
        XYSeries time = new XYSeries("time",false,true);
        for (int i = 0; i<data.length;++i) {
            time.add(i,((double)data[i])*voltageADCFactor);
        }

        timeSeries.removeAllSeries();
        timeSeries.addSeries(time);
    }
    private void calculateFourierDataSeries(byte [] data) {
        Complex [] cmplxData = new Complex[data.length];
        double [] fftResult;
        scala.collection.immutable.Seq<Object> transformed;
        for (int i = 0;i<data.length;++i)
        {
            cmplxData[i] = new Complex( data[i]/(double)messageLength,0.0);
        }
        Seq<Complex> dataToFFt = scala.collection.JavaConverters.asScalaBuffer(Arrays.asList(cmplxData)).toSeq();
        FourierTrans FourierTrans = new FourierTrans();
        transformed = FourierTrans.absfft((scala.collection.immutable.Seq<Complex>) dataToFFt);
        XYSeries fourier = new XYSeries("fourier",false,true);
        fftResult = scala.collection.JavaConverters.asJava(transformed).stream().mapToDouble(i -> (double) i).toArray();
        for (int i = 1; i<maxMeasuredFreq;++i) {
            fourier.add(i, fftResult[i]*voltageADCFactor/(double)messageLength);
        }
        fourierSeries.removeAllSeries();
        fourierSeries.addSeries(fourier);
    }

    @Override
    public int getListeningEvents() { return SerialPort.LISTENING_EVENT_DATA_RECEIVED; }

    @Override
    public int getPacketSize() { return messageLength; }

    @Override
    public void serialEvent(SerialPortEvent event)
    {
        byte[] newData = event.getReceivedData();
        if(newData.length != this.getPacketSize())
            System.out.println("Wrong message length");
        else {
            refreshTimeDataSeries(newData);
            calculateFourierDataSeries(newData);
            //System.out.println("GOT IT!");
        }
    }

}
