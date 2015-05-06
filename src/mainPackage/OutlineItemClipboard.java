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

package mainPackage;

import java.util.ArrayList;
import java.util.List;

public class OutlineItemClipboard {
	private ArrayList<OutlineItem> items;
	private ArrayList<IClipboardListener> listeners;
	
	public OutlineItemClipboard() {
		listeners = new ArrayList<>();
		items = new ArrayList<>();
	}

	public void replaceItems(List<OutlineItem> items) {
		this.items.clear();
		
		for (OutlineItem item : items) {
			this.items.add(new OutlineItem(item, null, true));
		}
		
		for (IClipboardListener listener : listeners) {
			listener.getNotification();
		}
	}
	
	public List<OutlineItem> getItems() {
		ArrayList<OutlineItem> result = new ArrayList<>();
		
		for (OutlineItem item : items) {
			OutlineItem newNode = new OutlineItem(item, null, true);
			result.add(newNode);
		}
		
		return result;
	}
	
	public boolean isEmpty() {
		return items.size() == 0;
	}
	
	public void addListener(IClipboardListener listener) {
		listeners.add(listener);
	}
	
	public void removeListener(IClipboardListener listener) {
		listeners.remove(listener);
	}
}
