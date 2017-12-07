package Main;

import backend.*;
import IntersectingPolygons_GUI.*;

import javax.swing.SwingUtilities;

public class Main {
	public static void main(String[] args){
		
		SwingUtilities.invokeLater(new Runnable() {
		public void run(){
			try{
				IP_Frame frame = new IP_Frame();
				frame.setVisible(true);
			}
			catch(Exception e){
				e.printStackTrace();
			}
		}		
	});
		
		
}
}
