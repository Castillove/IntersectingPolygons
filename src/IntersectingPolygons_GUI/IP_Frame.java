package IntersectingPolygons_GUI;

import java.awt.BorderLayout;
import java.awt.Color;
import java.awt.Font;
import java.awt.event.ActionEvent;
import java.awt.event.ActionListener;

import javax.swing.BorderFactory;
import javax.swing.JButton;
import javax.swing.JFrame;
import javax.swing.JLabel;
import javax.swing.JPanel;
import javax.swing.SwingConstants;

/*
 * Main window that displays Intersecting Polygons GUI.
 * Flow of screens are as follows:
 * 		screen 0: program controls
 * 		screen 1: inputting the first (blue) polygon
 * 		screen 2: inputting the second (green) polygon
 * 		screen 3: showing intersections via dual
 * */
public class IP_Frame extends JFrame implements ActionListener{
    private static final long serialVersionUID = -1511527241729280227L;
	JPanel contentPane = (JPanel) getContentPane();
	IP_PaintPanel paintBluePG;
	IP_PaintPanel paintGreenPG;
	IP_PaintDual paintDual;
	private JLabel lInstructions;
	private JButton bNext;
	private int screen;
	
	/*[CONSTRUCTOR] IP_Frame(): 
	 * Initializes the JFrame's state*/
	public IP_Frame(){
		setExtendedState(JFrame.MAXIMIZED_BOTH);
		setUndecorated(true);
		setDefaultCloseOperation(JFrame.EXIT_ON_CLOSE);	
		setLayout(new BorderLayout());
				
		lInstructions = new JLabel();
		lInstructions.setFont(new Font("Aharoni", Font.BOLD, 60));
		lInstructions.setForeground(Color.decode("#ffb700"));
		lInstructions.setHorizontalAlignment(SwingConstants.CENTER);
		
		bNext = new JButton();
		bNext.setFont(new Font("Aharoni", Font.BOLD, 60));
		bNext.setBackground(Color.DARK_GRAY);
		bNext.setForeground(Color.decode("#ffb700"));
		bNext.setBorderPainted(false);
		bNext.addActionListener(this);
		bNext.setEnabled(true);
			
		contentPane.setBackground(Color.BLACK);
		contentPane.add(bNext, BorderLayout.PAGE_END);
		contentPane.add(lInstructions, BorderLayout.PAGE_START);
		
		screen=0;
		chooseScreen();
	}

	/* chooseScreen(): 
	 * Use of a switch determines which of the screens (JPanels) to move on to
	 * Strings used for ease of debugging purposes, please do not remove*/
	private void chooseScreen(){
		switch (screen){
			case 0: System.out.println("CONTROLS");
					JLabel controls = new JLabel("left click to add point, right click to switch between points and lines");
					controls.setFont(new Font("Aharoni", Font.BOLD, 60));
					controls.setForeground(Color.DARK_GRAY);
					controls.setHorizontalAlignment(SwingConstants.CENTER);
					controls.setVerticalAlignment(SwingConstants.CENTER);
					contentPane.add(controls, BorderLayout.CENTER);
					lInstructions.setText("instructions");
					bNext.setText("click on me to continue");
					break;
			case 1: System.out.println("MAKE BLUE POLYGON");
					lInstructions.setText("draw a polygon");
					bNext.setText("next");
				 	paintBluePG = new IP_PaintPanel(Color.BLUE, null);
					paintBluePG.setOpaque(false);
					paintBluePG.setBorder(BorderFactory.createLineBorder(Color.DARK_GRAY));
					contentPane.add(paintBluePG, BorderLayout.CENTER);
					contentPane.validate();
					break;
			case 2: System.out.println("MAKE GREEN POLYGON");
					lInstructions.setText("draw another polygon");
					bNext.setText("next");
					
				 	paintGreenPG = new IP_PaintPanel(Color.GREEN, paintBluePG.getPaintedPoints());
					paintGreenPG.setOpaque(false);
					paintGreenPG.setBorder(BorderFactory.createLineBorder(Color.DARK_GRAY));
					contentPane.add(paintGreenPG, BorderLayout.CENTER);
					contentPane.validate();
					break;
			case 3: System.out.println("SHOW DUAL");
					lInstructions.setText("intersection via projective duality");
					bNext.setText("reset");
					screen=0;	//Want the "draw a polygon" screen to appear upon reset
					
					paintDual = new IP_PaintDual(paintBluePG.getPointList(), paintGreenPG.getPointList());
					paintDual.setOpaque(false);
					paintDual.setBorder(BorderFactory.createLineBorder(Color.DARK_GRAY));
					contentPane.add(paintDual, BorderLayout.CENTER);
					contentPane.validate();
					paintDual.repaint();
		}
	}
	@Override
	/*[OVERRIDE] actionPerformed(ActionEvent arg0)
	 * Dictates what will happen when the button is pressed:
	 * 		First checks whether or not enough points for a polygon 
	 * 			were inputed (only for screens 1 and 2
	 * 		If not, gets ready to move onto next screen*/
	public void actionPerformed(ActionEvent arg0){
		int checkNumPoints = 100;
		if(screen == 1 || screen == 2){
			checkNumPoints = (screen == 1 ? paintBluePG.getPaintedPoints().size() :
				paintGreenPG.getPaintedPoints().size());
		}
		
		if(checkNumPoints < 3){
			bNext.setText("not enough points, need at least 3");
		}
		else{
			System.out.println("REMOVING PREVIOUS JPANEL");
			contentPane.remove(contentPane.getComponentCount()-1);
			screen++;
			System.out.println("SCREEN " + screen);
			chooseScreen();
		}
		
	}
}
