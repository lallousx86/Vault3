/*
  Vault 3
  (C) Copyright 2013, Eric Bergman-Terrell
  
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

package mainPackage;

import java.util.HashMap;
import java.util.Map;

import org.eclipse.swt.graphics.FontData;

import commonCode.IPlatform.PlatformEnum;

import fonts.FontList;
import fonts.IFont;
import fonts.SWTFont;

public class FontListInitializer {
	public static void initialize() {
		Map<String, IFont> availableFonts = enumerateAvailableFonts();
		PlatformEnum platform = Globals.getPlatform();
		
		Globals.getLogger().info("FontListInitializer:");
		Globals.getLogger().info(String.format("Platform: %s", platform.toString()));
		
		FontList.setAvailableFonts(availableFonts);
		FontList.setPlatform(platform);
	}
	
	private static Map<String, IFont> enumerateAvailableFonts() {
		Map<String, IFont> map = new HashMap<>();

		boolean[] scalableValues = new boolean[] { true, false };

		Globals.getLogger().info("FontList.enumarateAvailableFonts:\r\n\r\n");
		
		for (boolean scalable : scalableValues) {
	        FontData[] fontList = Globals.getMainApplicationWindow().getShell().getDisplay().getFontList(null, scalable);
	        
	        for (FontData fontData : fontList) {
	        	SWTFont swtFont = new SWTFont(fontData.getName(), Globals.getPlatform(), fontData.toString());
	        	map.put(swtFont.getName(), swtFont);
	        	
	        	Globals.getLogger().info(
	        			String.format("Platform: %s Font: %s Data: %s Scalable: %s", 
	        						  Globals.getPlatform().toString(), swtFont.getName(), swtFont.getData(), String.valueOf(scalable))
	        						    );
	        }
        }
		
		return map;
	}
}
