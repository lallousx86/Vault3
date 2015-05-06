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

import java.util.Stack;

import org.eclipse.core.runtime.Assert;
import org.xml.sax.*;
import org.xml.sax.helpers.*;

public class ImportDefaultHandler extends DefaultHandler {
	private OutlineItemWithTempText outlineItem;
	private StringBuilder elementText;
	Stack<OutlineItemWithTempText> stack;
	private boolean newline;
	
	private final static String TEXTELEMENTNAME = "TEXT"; 
	private final static String VAULTELEMENTNAME = "VAULT";
	private final static String THEPHOTOPROGRAMELEMENTNAME = "THEPHOTOPROGRAM";
	private final static String ITEMELEMENTNAME = "ITEM";
	private final static String TITLEELEMENTNAME = "TITLE";
	private final static String PHOTOELEMENTNAME = "PHOTO";

	public ImportDefaultHandler() {
	}

	public OutlineItem getOutlineItem() {
		return outlineItem.getOutlineItem();
	}

	@Override
	public void startDocument() throws SAXException {
		super.startDocument();

		stack = new Stack<>();
		outlineItem = null;
		elementText = null;
	}

	@Override
	public void endDocument() throws SAXException {
		super.endDocument();
		
		Assert.isTrue(stack.isEmpty());
	}

	@Override
	public void startElement(String uri, String localName, String name,
			Attributes attributes) throws SAXException {
		super.startElement(uri, localName, name, attributes);
		
		elementText = new StringBuilder();
		
		if (name.equals(TEXTELEMENTNAME)) {
			String type = attributes.getValue("type");
			
			newline = type.equals("newline");
		}
		else if (name.equals(ITEMELEMENTNAME)) {
			OutlineItem parent = stack.peek().getOutlineItem();
			
			OutlineItemWithTempText child = new OutlineItemWithTempText();
			child.getOutlineItem().setParent(parent);
			
			parent.addChild(child.getOutlineItem());

			stack.push(child);
		}
		else if (name.equals(PHOTOELEMENTNAME)) {
			String photoPath = attributes.getValue("path");
			
			if (photoPath != null) {
				OutlineItem currentItem = stack.peek().getOutlineItem();
				currentItem.setPhotoPath(photoPath);
				
				// Globals.getLogger().info(String.format("photoPath: %s", photoPath));
			}
		}
		else if (name.equals(VAULTELEMENTNAME) || name.equals(THEPHOTOPROGRAMELEMENTNAME)) {
			outlineItem = new OutlineItemWithTempText();
			stack.push(outlineItem);
		}
	}

	@Override
	public void endElement(String uri, String localName, String name)
			throws SAXException {
		super.endElement(uri, localName, name);

		if (name.equals(ITEMELEMENTNAME)) {
			OutlineItemWithTempText outlineItem = stack.pop();
			
			outlineItem.getOutlineItem().setText(outlineItem.getTempText().toString());
		}
		else if (name.equals(TITLEELEMENTNAME)) {
			stack.peek().getOutlineItem().setTitle(elementText.toString());
		} 
		else if (name.equals(TEXTELEMENTNAME)) {
			if (!newline) {
				stack.peek().getTempText().append(elementText);
			}
			else {
				stack.peek().getTempText().append(PortabilityUtils.getNewLine());
			}
		}
		else if (name.equals(VAULTELEMENTNAME) || name.equals(THEPHOTOPROGRAMELEMENTNAME)) {
			stack.pop();
		}
	}

	@Override
	public void characters(char[] ch, int start, int length)
			throws SAXException {
		super.characters(ch, start, length);
		
		String currentText = new String(ch, start, length);
		
		if (currentText.length() > 0) {
			elementText.append(currentText);
		}
	}
}
