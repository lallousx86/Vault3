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

import java.util.ArrayList;
import java.util.List;

import org.eclipse.jface.dialogs.IDialogConstants;
import org.eclipse.swt.printing.PrintDialog;
import org.eclipse.swt.printing.Printer;
import org.eclipse.swt.printing.PrinterData;
import org.eclipse.swt.widgets.Shell;

import commonCode.IPlatform.PlatformEnum;

public class Printing {
	public static List<VaultPrinter.TextWithFont> getPrintText(OutlineItem outlineItem) {
		List<VaultPrinter.TextWithFont> textToPrint = new ArrayList<>();
		
		String defaultFont = Globals.getPreferenceStore().getString(PreferenceKeys.DefaultTextFont);
		
		VaultPrinter.TextWithFont titleText = new VaultPrinter.TextWithFont(String.format("%s\n\n", outlineItem.getTitle()), defaultFont, null);
		textToPrint.add(titleText);
		
		VaultPrinter.TextWithFont itemText = new VaultPrinter.TextWithFont(outlineItem.getText(), outlineItem.getFontString(), outlineItem.getPhotoPath());
		textToPrint.add(itemText);
		
		if (textToPrint.size() > 0) {
			VaultPrinter.TextWithFont newLines = new VaultPrinter.TextWithFont("\n\n", defaultFont, null);
			textToPrint.add(newLines);
		}
		
		for (OutlineItem childItem : outlineItem.getChildren()) {
			List<VaultPrinter.TextWithFont> childList = getPrintText(childItem);
			textToPrint.addAll(childList);
		}
		
		return textToPrint;
	}
	
	public static boolean canPrint() {
		List<OutlineItem> selectedItems = Globals.getVaultTreeViewer().getSelectedItems();
		
		return selectedItems.size() > 0;
	}
	
	public static void print(Shell shell) {
		if (canPrint()) {
			PrintScopeDialog printScopeDialog = null;
			boolean cancelled = false;
			
			if (Globals.getPlatform() == PlatformEnum.MacOSX) {
				printScopeDialog = new PrintScopeDialog(shell);
				cancelled = printScopeDialog.open() != IDialogConstants.OK_ID;
			}
			
			if (!cancelled) {
				PrintDialog printDialog = new PrintDialog(shell);
				printDialog.setText("Print");
				printDialog.setScope(PrinterData.SELECTION);
				
				PrinterData printerData = printDialog.open();
				
				if (printerData != null) {
					Printer printer = new Printer(printerData);
					
					List<OutlineItem> selectedItems;
					
					if (printScopeDialog != null) {
						// Use values specified by user when PrintScopeDialog was displayed.
						
						printDialog.setScope(printScopeDialog.getScope());
						printDialog.setStartPage(printScopeDialog.getStartPage());
						printDialog.setEndPage(printScopeDialog.getEndPage());
					}
	
					// Only print all topics if user chooses the All radio button in the print dialog.
					if (printDialog.getScope() == PrinterData.SELECTION || printDialog.getScope() == PrinterData.PAGE_RANGE) {
						selectedItems = Globals.getVaultTreeViewer().getSelectedItems();
					}
					else {
						selectedItems = Globals.getVaultTreeViewer().getAllItems();
					}
		
					List<VaultPrinter.TextWithFont> textToPrint = new ArrayList<>();
		
					for (OutlineItem outlineItem : selectedItems) {
						textToPrint.addAll(getPrintText(outlineItem));
					}
					
					VaultPrinter vaultPrinter = new VaultPrinter(printer, textToPrint, Globals.getVaultDocument().getFileName());
	
					// If user specified a page range, use it.
					if (printDialog.getScope() == PrinterData.PAGE_RANGE && printDialog.getStartPage() >= 1 && printDialog.getEndPage() >= printDialog.getStartPage()) {
						vaultPrinter.setPageRange(new VaultPrinter.PageRange(printDialog.getStartPage(), printDialog.getEndPage()));
					}
					
					vaultPrinter.print();
				}
			}
		}
	}
}
