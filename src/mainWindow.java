import javax.swing.*;
import java.awt.*;

public class mainWindow {

    public mainWindow()
    {
        JFrame frame = new JFrame();
        JPanel panel = new JPanel();
        panel.setBorder(BorderFactory.createEmptyBorder(300,300,300,300));
        panel.setLayout(new GridLayout(0,1));
        frame.add(panel,BorderLayout.CENTER);
        frame.setDefaultCloseOperation((JFrame.EXIT_ON_CLOSE));
        frame.setTitle("Accelerometer Fourier Analysis");
        frame.pack();
        frame.setVisible(true);
    }
    public static void main(String[] args)
    {
        new mainWindow();
    }
}
