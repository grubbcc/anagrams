
import java.util.prefs.Preferences;

public class Pass {

	public static void main(String[] args) {
		
		
		String password = args.length > 0 ? args[0] : "6mari2]92]mm";
	
		Preferences prefs = Preferences.userRoot().node("client");
		prefs.put("password", password);

	
	}


}