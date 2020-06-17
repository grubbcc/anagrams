/**
* A custom HTML-enabled frame for displaying pop-up messages
* Adapted from Jean-Marc Astesana's answer at https://stackoverflow.com/a/33446134/3736508
*/

import java.awt.Color;
import java.awt.Font;
import java.awt.Desktop;
import javax.swing.JEditorPane;
import javax.swing.JLabel;
import javax.swing.JOptionPane;
import javax.swing.event.HyperlinkEvent;
import javax.swing.event.HyperlinkListener;
import java.net.URI;

public class HypertextMessage extends JEditorPane {

	/**
	*
	*/

	public HypertextMessage(String htmlBody) {
		super("text/html", "<html><body style=\"" + getStyle() + "\"><p>" + htmlBody + "</p></body></html>");
		addHyperlinkListener(new HyperlinkListener() {
			@Override
			public void hyperlinkUpdate(HyperlinkEvent e) {
				if (e.getEventType().equals(HyperlinkEvent.EventType.ACTIVATED)) {

					try	{
						URI uri = new URI(e.getURL().toString());
						Desktop dt = Desktop.getDesktop();
						dt.browse(uri);
					}

					catch (Exception e2) {
						e2.printStackTrace();
					}
				}
			}
		});
		setEditable(false);
		setBorder(null);
	}
	
	/**
	*
	*/

	static StringBuffer getStyle() {
		// for copying style
		JLabel label = new JLabel();
		Font font = label.getFont();
		Color color = label.getBackground();

		// create some css from the label's font
		StringBuffer style = new StringBuffer("font-family:" + font.getFamily() + ";");
		style.append("font-weight:" + (font.isBold() ? "bold" : "normal") + ";");
		style.append("font-size:" + font.getSize() + "pt;");
		style.append("text-align:center;");
		style.append("background-color: rgb(" + color.getRed() + "," + color.getGreen() + "," + color.getBlue() + ");");
		return style;
	}
}
