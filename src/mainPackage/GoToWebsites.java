/*
  Vault 3
  (C) Copyright 2009, Eric Bergman-Terrell
  
  This file is part of Vault 3.

    Vault 3 is free software: you can redistribute it and/or modify
    it under the terms of the GNU General Public License as published by
    the Free Software Foundation, either version 3 of the License, or
    (at your option) any later version.

    Vault 3 is distributed in the hope that it will be useful,
    but WITHOUT ANY WARRANTY; without even the implied warranty of
    MERCHANTABILITY or FITNESS FOR A PARTICULAR PURPOSE.  See the
    GNU General Public License for more details.

    You should have received a copy of the GNU General Public License
    along with Vault 3.  If not, see <http://www.gnu.org/licenses/>.
*/

/**
 * 
 */
package mainPackage;

import java.io.File;
import java.io.UnsupportedEncodingException;
import java.net.URLDecoder;
import java.text.MessageFormat;
import java.util.ArrayList;
import java.util.List;
import java.util.regex.Matcher;
import java.util.regex.Pattern;

import org.eclipse.jface.dialogs.MessageDialog;
import org.eclipse.swt.graphics.Image;
import org.eclipse.swt.program.Program;
import org.eclipse.swt.widgets.Shell;

/**
 * @author Eric Bergman-Terrell
 *
 */
public class GoToWebsites {
	public static final String encoding = "UTF-8";
	
	public static void run(Shell shell) {
		Pattern pattern = Pattern.compile(Globals.getPreferenceStore().getString(PreferenceKeys.URLRegex));

		String itemText = Globals.getVaultTextViewer().getTextWidget().getText();
		
		Matcher matcher = pattern.matcher(itemText);
		
		List<String> urls = new ArrayList<>();
		
		while (matcher.find()) {
			String url = itemText.substring(matcher.start(), matcher.end());
			
			if (!urls.contains(url)) {
				urls.add(url);
			}
		}
		
		if (urls.size() > 0) {
			if (urls.size() == 1) {
				GoToWebsites.launch(urls.get(0), Globals.getMainApplicationWindow().getShell());
			}
			else {
				GoToWebsitesDialog goToWebsitesDialog = new GoToWebsitesDialog(shell, urls);
				goToWebsitesDialog.open();
			}
		}
	}
	
	private static String decodeAndSubstituteUrl(String url) {
		String result = url;
		
		try {
			result = URLDecoder.decode(result, encoding);
		} catch (UnsupportedEncodingException e) {
			e.printStackTrace();
		}

		final String fileProtocol = "file:///";
		
		if (result.startsWith(fileProtocol)) {
			String filePath = result.substring(fileProtocol.length());
			
			String photoPath = PhotoUtils.getPhotoPath(filePath);
			
			if (photoPath != null && photoPath.length() > 0) {
				result = fileProtocol + photoPath;
			}
			
			// Drop the file:/// prefix, otherwise the program will not be launchable on Linux.
			result = result.substring(fileProtocol.length());
			
			if (!new File(result).exists()) {
				Image icon = Globals.getImageRegistry().get(Globals.IMAGE_REGISTRY_VAULT_ICON);

				String message = MessageFormat.format("File \"{0}\" does not exist.", result);
				MessageDialog messageDialog = new MessageDialog(Globals.getMainApplicationWindow().getShell(), StringLiterals.ProgramName, icon, message, MessageDialog.INFORMATION, new String[] { "&OK" }, 0);
				messageDialog.open();

				result = null;
			}
		}
		
		return result;
	}

	public static void launch(String url, Shell shell) {
		url = decodeAndSubstituteUrl(url);
		
		if (url != null) {
			Program.launch(url);
		}
	}
	
}
