package Main;

import javax.swing.SwingUtilities;

import IntersectingPolygons_GUI.IP_Frame;

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
