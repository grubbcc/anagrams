import java.awt.Color;
import java.awt.Component;
import java.awt.Dimension;
import java.util.Enumeration;
import java.util.LinkedHashMap;
import javax.swing.*;

/**
*
* Adapted from sharath's code, http://java-gui.blogspot.com/2011/07/jcolorcombobox-jcombobox-as-color.html
*/

public class JColorComboBox extends JComboBox {

	static LinkedHashMap<String, Color> colors = new LinkedHashMap<String, Color>();
	
	/**
	*
	*/

	public JColorComboBox() {
		super();
		DefaultComboBoxModel<String> model = new DefaultComboBoxModel<String>();
		for(Object colorName : addColors().keySet()) {
			model.addElement(colorName.toString());
		}
		setModel(model);
		setPreferredSize(new Dimension(60, 20));
		
		setRenderer(new ColorRenderer<String>());
		setMaximumRowCount(20);
		setOpaque(true);
		setSelectedIndex(0);
	}


	/**
	*
	*/
	
	@Override
	public void setSelectedItem(Object o) {
		super.setSelectedItem(o);

		setBackground(Color.decode((String)o));

	}
	

	
	/**
	*
	*/

	private LinkedHashMap addColors() {
		
		String[] colorCodes = {"#CD5C5C", "#F08080", "#FA8072", "#E9967A", "#FFA07A", "#DC143C", "#FF0000", "#B22222", "#8B0000", "#FFC0CB", "#FFB6C1", "#FF69B4", "#FF1493", "#C71585", "#DB7093", "#FFA07A", "#FF7F50", "#FF6347", "#FF4500", "#FF8C00", "#FFA500", "#FFD700", "#FFFF00", "#FFFFE0", "#FFFACD", "#FAFAD2", "#FFEFD5", "#FFE4B5", "#FFDAB9", "#EEE8AA", "#F0E68C", "#BDB76B", "#E6E6FA", "#D8BFD8", "#DDA0DD", "#EE82EE", "#DA70D6", "#FF00FF", "#FF00FF", "#BA55D3", "#9370DB", "#663399", "#8A2BE2", "#9400D3", "#9932CC", "#8B008B", "#800080", "#4B0082", "#6A5ACD", "#483D8B", "#7B68EE", "#ADFF2F", "#7FFF00", "#7CFC00", "#00FF00", "#32CD32", "#98FB98", "#90EE90", "#00FA9A", "#00FF7F", "#3CB371", "#2E8B57", "#228B22", "#008000", "#006400", "#9ACD32", "#6B8E23", "#808000", "#556B2F", "#66CDAA", "#8FBC8B", "#20B2AA", "#008B8B", "#008080", "#00FFFF", "#00FFFF", "#E0FFFF", "#AFEEEE", "#7FFFD4", "#40E0D0", "#48D1CC", "#00CED1", "#5F9EA0", "#4682B4", "#B0C4DE", "#B0E0E6", "#ADD8E6", "#87CEEB", "#87CEFA", "#00BFFF", "#1E90FF", "#6495ED", "#7B68EE", "#4169E1", "#0000FF", "#0000CD", "#00008B", "#000080", "#191970", "#FFF8DC", "#FFEBCD", "#FFE4C4", "#FFDEAD", "#F5DEB3", "#DEB887", "#D2B48C", "#BC8F8F", "#F4A460", "#DAA520", "#B8860B", "#CD853F", "#D2691E", "#8B4513", "#A0522D", "#A52A2A", "#800000", "#FFFFFF", "#FFFAFA", "#F0FFF0", "#F5FFFA", "#F0FFFF", "#F0F8FF", "#F8F8FF", "#F5F5F5", "#FFF5EE", "#F5F5DC", "#FDF5E6", "#FFFAF0", "#FFFFF0", "#FAEBD7", "#FAF0E6", "#FFF0F5", "#FFE4E1", "#DCDCDC", "#D3D3D3", "#C0C0C0", "#A9A9A9", "#808080", "#696969", "#778899", "#708090", "#2F4F4F", "#000000"};
		
		for(String colorCode : colorCodes) {
			colors.put(colorCode, Color.decode(colorCode));
		}

		return colors;
	}

	/**
	*
	*/

	class ColorRenderer<String> extends JLabel implements javax.swing.ListCellRenderer {
		
		/**
		*
		*/
		
		public ColorRenderer() {
			this.setOpaque(true);
		}
		
		/**
		*
		*/
			
		public Component getListCellRendererComponent(JList list, Object key, int index, boolean isSelected, boolean cellHasFocus) {

			Color color = colors.get(key);
			setPreferredSize(new Dimension(60, 6));

			if(isSelected) {
				setBorder(BorderFactory.createEtchedBorder());
			}			
			else {
				setBorder(null);
			}
			
			setBackground(color);
			setForeground(color);
			
			return this;
		}
	}
}
