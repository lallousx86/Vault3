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

import java.io.File;
import java.text.MessageFormat;
import java.text.SimpleDateFormat;
import java.util.Date;
import java.util.Stack;
import java.util.regex.Matcher;
import java.util.regex.Pattern;

import mainPackage.TextActions.DisplayUrlAction;
import mainPackage.TextActions.PasteAction;

import org.eclipse.jface.action.MenuManager;
import org.eclipse.jface.action.Separator;
import org.eclipse.jface.dialogs.IDialogConstants;
import org.eclipse.jface.dialogs.MessageDialog;
import org.eclipse.jface.preference.PreferenceStore;
import org.eclipse.jface.text.*;
import org.eclipse.jface.viewers.ISelectionChangedListener;
import org.eclipse.jface.viewers.IStructuredSelection;
import org.eclipse.jface.viewers.SelectionChangedEvent;
import org.eclipse.swt.SWT;
import org.eclipse.swt.custom.ExtendedModifyEvent;
import org.eclipse.swt.custom.ExtendedModifyListener;
import org.eclipse.swt.custom.StyleRange;
import org.eclipse.swt.custom.StyledText;
import org.eclipse.swt.dnd.Clipboard;
import org.eclipse.swt.dnd.RTFTransfer;
import org.eclipse.swt.dnd.TextTransfer;
import org.eclipse.swt.events.MenuEvent;
import org.eclipse.swt.events.MenuListener;
import org.eclipse.swt.graphics.Color;
import org.eclipse.swt.graphics.Font;
import org.eclipse.swt.graphics.FontData;
import org.eclipse.swt.graphics.Image;
import org.eclipse.swt.graphics.Point;
import org.eclipse.swt.graphics.RGB;
import org.eclipse.swt.widgets.Composite;
import org.eclipse.swt.widgets.Display;
import org.eclipse.swt.widgets.FileDialog;
import org.eclipse.swt.widgets.FontDialog;
import org.eclipse.swt.widgets.Shell;

import fonts.FontList;
import fonts.SWTFont;

/**
 * @author Eric Bergman-Terrell
 *
 */
public class VaultTextViewer extends TextViewer implements ISelectionChangedListener {
	private String previousInsertTextFileFilterPath = null;
	
	private boolean doNotRecordUndoInfo = false, menusArmed = false;

	private boolean isModified;
	
	private OutlineItem outlineItem; 
	
	private boolean usingNonDefaultFont = false;

	private class UndoInfo {
		private int start, length;
		private String replacedText;

		public UndoInfo(int start, int length, String replacedText) {
			this.start = start;
			this.length = length;
			this.replacedText = replacedText;
		}
	}

	private Stack<UndoInfo> undoBuffer = new Stack<>();

	private Pattern[] searchPatterns;

	public void setSearchPatterns(Pattern[] searchPatterns, boolean searchHitsFound) {
		this.searchPatterns = searchPatterns;

		(Globals.getMainApplicationWindow().getAction(SearchActions.NextSearchHitAction.class)).setEnabled(searchHitsFound);
		(Globals.getMainApplicationWindow().getAction(SearchActions.PreviousSearchHitAction.class)).setEnabled(searchHitsFound);
		(Globals.getMainApplicationWindow().getAction(SearchActions.NextSearchItemAction.class)).setEnabled(searchHitsFound);
		(Globals.getMainApplicationWindow().getAction(SearchActions.PreviousSearchItemAction.class)).setEnabled(searchHitsFound);
		(Globals.getMainApplicationWindow().getAction(SearchActions.ClearSearchAction.class)).setEnabled(searchHitsFound);
	}
	
	public VaultTextViewer(Composite parent, int styles) {
		super(parent, styles);
		
		isModified = false;
		
		// Limit text to 64 MB. Large amounts of text in a topic will create significant performance issues.
		getTextWidget().setTextLimit(1024 * 1024 * 64);
		
		String defaultTextFont = Globals.getPreferenceStore().getString(PreferenceKeys.DefaultTextFont);
		
		if (defaultTextFont.length() == 0) {
			defaultTextFont = FontUtils.fontListToString(getTextWidget().getFont().getFontData());
			Globals.getPreferenceStore().setValue(PreferenceKeys.DefaultTextFont, defaultTextFont);
		}
		
		getTextWidget().setEnabled(false);
		
		getTextWidget().addExtendedModifyListener(new ExtendedModifyListener() {
			@Override
			public void modifyText(ExtendedModifyEvent event) {
				if (!doNotRecordUndoInfo) {
					undoBuffer.push(new UndoInfo(event.start, event.length, event.replacedText));
				}
			}
		});

		MainApplicationWindow mainApplicationWindow = Globals.getMainApplicationWindow();

		final MenuManager menuManager = new MenuManager();
		menuManager.add(mainApplicationWindow.getAction(TextActions.UndoAction.class));
		menuManager.add(new Separator());
		menuManager.add(mainApplicationWindow.getAction(TextActions.CutAction.class));
		menuManager.add(mainApplicationWindow.getAction(TextActions.CopyAction.class));
		menuManager.add(mainApplicationWindow.getAction(TextActions.PasteAction.class));
		menuManager.add(mainApplicationWindow.getAction(TextActions.SelectAllAction.class));
		menuManager.add(new Separator());
		menuManager.add(mainApplicationWindow.getAction(TextActions.FindAction.class));
		menuManager.add(mainApplicationWindow.getAction(TextActions.ReplaceAction.class));
		menuManager.add(new Separator());
		menuManager.add(mainApplicationWindow.getAction(TextActions.InsertDateAction.class));
		menuManager.add(mainApplicationWindow.getAction(TextActions.InsertTimeAction.class));
		menuManager.add(mainApplicationWindow.getAction(TextActions.InsertDateAndTimeAction.class));
		menuManager.add(new Separator());
		menuManager.add(mainApplicationWindow.getAction(TextActions.InsertTextFileAction.class));
		menuManager.add(new Separator());
		menuManager.add(mainApplicationWindow.getAction(TextActions.InsertUrlAction.class));
		menuManager.add(mainApplicationWindow.getAction(TextActions.DisplayUrlAction.class));
		menuManager.add(mainApplicationWindow.getAction(TextActions.GoToWebsitesAction.class));
		menuManager.add(new Separator());
		menuManager.add(mainApplicationWindow.getAction(TextActions.SetFontAction.class));
		
		getControl().setMenu(menuManager.createContextMenu(parent));

		getControl().getMenu().addMenuListener(new MenuListener() {
			@Override
			public void menuHidden(MenuEvent e) {
				Globals.getMainApplicationWindow().setStatusLineMessage("");
			}

			@Override
			public void menuShown(MenuEvent menuEvent) {
				if (!menusArmed) {
					MenuUtils.armAllMenuItems(menuManager);
					menusArmed = true;
				}

				DisplayUrlAction displayUrlAction = (DisplayUrlAction) Globals.getMainApplicationWindow().getAction(TextActions.DisplayUrlAction.class);
				displayUrlAction.setEnabled(canDisplayUrl());
				
				PasteAction pasteAction = (PasteAction) Globals.getMainApplicationWindow().getAction(TextActions.PasteAction.class);
				pasteAction.setEnabled();
				
				// Move the cursor over a bit to avoid selecting the first menu item on Ubuntu.
				Point cursorLocation = menuEvent.display.getCursorLocation();
				
				Display.getCurrent().setCursorLocation(cursorLocation.x + 1, cursorLocation.y + 1);
				
				Point mappedPoint = Display.getCurrent().map(null, getTextWidget(), cursorLocation);
				
				// Try to move the cursor to the current mouse position.
				
				int offset = -1;
				
				try {
					offset = getTextWidget().getOffsetAtLocation(mappedPoint);
				}
				catch (Throwable ex) {
					// The cursor was probably to the right of the text on a given line. Get the offset at the beginning
					// of the line by moving the point all the way to the left.
					Point newPoint = new Point(0, mappedPoint.y);
					
					try {
						// If we got the offset, move to the end of the line.
						offset = getTextWidget().getOffsetAtLocation(newPoint);
						int lineIndex = getTextWidget().getLineAtOffset(offset);
						offset += getTextWidget().getLine(lineIndex).length();
					}
					catch (Throwable ex2) {
						// Otherwise, create an offset to the end of the entire text.
						offset = getTextWidget().getText().length();
					}
				}
				
				// Set the cursor if possible, but do not set the cursor if it's over selected text.
				if (offset != -1) {
					boolean overSelectedText = false;
					
					if (getTextWidget().getSelectionCount() > 0) {
						Point selectedPoint = getTextWidget().getSelection();
						
						overSelectedText = offset >= selectedPoint.x && offset < selectedPoint.y; 
					}
					
					if (!overSelectedText) {
						getTextWidget().setCaretOffset(offset);
					}
				}
			}
		});

		setBackgroundColor();
	}

	public void setBackgroundColor() {
		PreferenceStore preferenceStore = Globals.getPreferenceStore();
		
		RGB textBackgroundColor = new RGB(preferenceStore.getInt(PreferenceKeys.TextBackgroundRed),
				  						  preferenceStore.getInt(PreferenceKeys.TextBackgroundGreen),
				  						  preferenceStore.getInt(PreferenceKeys.TextBackgroundBlue));
		
		StyledText textWidget = getTextWidget();
		
		Point selection = textWidget.getSelection();
		
		textWidget.setFocus();
		textWidget.setSelection(0, 0);
		
		textWidget.setBackground(Globals.getColorRegistry().get(textBackgroundColor));
		textWidget.setSelection(selection);
	}
	
	private String getFontString() {
		String fontString = null;
		
		if (getOutlineItem() != null) {
			fontString = getOutlineItem().getFontString();
		}
		
		if (fontString == null) {
			if (Globals.getPreferenceStore().getString(PreferenceKeys.DefaultTextFont).length() > 0)
			{
				fontString = Globals.getPreferenceStore().getString(PreferenceKeys.DefaultTextFont);
			}
		}
		
		return fontString;
	}
	
	/***
	 * Specify the font and color for the text. Use the outline item's font and color if it has one, otherwise
	 * use the default font and color.
	 */
	public void setFontAndColor(boolean forceColorChangeToBeVisible) {
		String newFontString = getFontString();

		String currentFontString = FontUtils.fontListToString(getTextWidget().getFont().getFontData());
		
		if (newFontString != null && !currentFontString.equals(newFontString)) {
			FontData fontList[] = FontUtils.stringToFontList(newFontString);
			
			Font previousFont = getTextWidget().getFont();
			
			Font font = new Font(getTextWidget().getDisplay(), fontList);
			getTextWidget().setFont(font);
			
			if (usingNonDefaultFont) {
				previousFont.dispose();
			}
		
			usingNonDefaultFont = true;
		}
		
		RGB rgb = null;
		
		if (getOutlineItem() != null) {
			rgb = getOutlineItem().getRGB();
		}

		// On windows, color change does not always take effect if text is loaded. For instance, if there is text and the cursor
		// is several lines below the text, the text change will not be visible when getTextWidget().setForegroundColor is called.
		
		// Save text and caret position.
		int savedCaretOffset = getTextWidget().getCaretOffset();
		String savedText = getTextWidget().getText();
		
		if (forceColorChangeToBeVisible) {
			getTextWidget().setText("");
		}
		
		if (rgb != null) {
			Color color = Globals.getColorRegistry().get(rgb);
			getTextWidget().setForeground(color);
		}
		else {
			int red   = Globals.getPreferenceStore().getInt(PreferenceKeys.DefaultTextFontRed);
			int green = Globals.getPreferenceStore().getInt(PreferenceKeys.DefaultTextFontGreen);
			int blue  = Globals.getPreferenceStore().getInt(PreferenceKeys.DefaultTextFontBlue);
			
			getTextWidget().setForeground(Globals.getColorRegistry().get(red, green, blue));
		}
		
		if (forceColorChangeToBeVisible) {
			// Restore text and caret position.
			getTextWidget().setText(savedText);
			getTextWidget().setCaretOffset(savedCaretOffset);
		}
	}
	
	public void setFontAndColor() {
		setFontAndColor(false);
	}
	
	
	public void setOutlineItem(OutlineItem outlineItem) {
		saveChanges();
		
		undoBuffer.clear();
		
		this.outlineItem = outlineItem;
		isModified = false;
		
    	Document document = new Document();

    	setFontAndColor();

    	if (outlineItem != null) {
	    	document.set(outlineItem.getText());
		}
    	else {
    		document.set("");
    	}
    	
    	document.addDocumentListener(new DocumentListener());
    	setDocument(document);
    	
    	highlightSearchHits();
    	
    	if (Globals.getFindAndReplaceDialog() != null) {
    		Globals.getFindAndReplaceDialog().UpdateFindReplaceDocumentAdapter();
    	}
	}

	public OutlineItem getOutlineItem() {
		return outlineItem;
	}

	public void saveChanges() {
		if (outlineItem != null && isModified) {
			IDocument document = getDocument();
			
			outlineItem.setText(document.get());
			
			isModified = false;
		}
	}
	
	private boolean isEnabledAndEditable() {
		StyledText textWidget = getTextWidget();
		return textWidget.getEnabled() && textWidget.getEditable();
	}
	
	public boolean canInsertDate() {
		return isEnabledAndEditable();
	}
	
	public void insertDate() {
		if (canInsertDate()) {
			String dateText = SimpleDateFormat.getDateInstance().format(new Date());
			getTextWidget().insert(dateText);
		}
	}
	
	public boolean canInsertTime() {
		return isEnabledAndEditable();
	}
	
	public void insertTime() {
		if (canInsertTime()) {
			String timeText = SimpleDateFormat.getTimeInstance().format(new Date());
			getTextWidget().insert(timeText);
		}
	}
	
	public boolean canInsertDateAndTime() {
		return isEnabledAndEditable();
	}
	
	public void insertDateAndTime() {
		if (canInsertDateAndTime()) {
			Date now = new Date();
			String dateAndTimeText = SimpleDateFormat.getDateTimeInstance().format(now);
			getTextWidget().insert(dateAndTimeText);
		}
	}
	
	public boolean canInsertUrl() {
		return isEnabledAndEditable();
	}
	
	public void insertUrl() {
		if (canInsertUrl()) {
			InsertUrlDialog insertUrlDialog = new InsertUrlDialog(Globals.getMainApplicationWindow().getShell());
				
			if (insertUrlDialog.open() == IDialogConstants.OK_ID) {
				String url = insertUrlDialog.getUrl();
								
				getTextWidget().insert(url);
			}
		}
	}
	
	public boolean canDisplayUrl() {
		return getSpecifiedUrl() != null;
	}
	
	private String getSpecifiedUrl() {
		String specifiedUrl = null;
		
		if (isEnabledAndEditable()) {
			Point cursorLocation = Display.getCurrent().getCursorLocation();
			Point mappedPoint = Display.getCurrent().map(null, getTextWidget(), cursorLocation);
			
			// Try to move the cursor to the current mouse position.
			
			int caretOffset = getTextWidget().getCaretOffset();
			
			try {
				caretOffset = getTextWidget().getOffsetAtLocation(mappedPoint);
			}
			catch (Throwable ex) {
				ex.printStackTrace();
			}

			if (caretOffset > -1) {
				Pattern pattern = Pattern.compile(Globals.getPreferenceStore().getString(PreferenceKeys.URLRegex));
	
				String itemText = getTextWidget().getText();
				
				Matcher matcher = pattern.matcher(itemText);
				
				while (matcher.find()) {
					if (matcher.start() <= caretOffset && matcher.end() >= caretOffset) {
						specifiedUrl = itemText.substring(matcher.start(), matcher.end());
						break;
					}
				}
			}
		}
		
		return specifiedUrl;
	}
	
	public void displayUrl() {
		if (canDisplayUrl()) {
			GoToWebsites.launch(getSpecifiedUrl(), Globals.getMainApplicationWindow().getShell());
		}
	}

	public boolean canUndo() {
		return isEnabledAndEditable() && undoBuffer.size() > 0;
	}
	
	public void undo() {
		if (canUndo()) {
			UndoInfo undoInfo = undoBuffer.pop();

			try
			{
				doNotRecordUndoInfo = true;
				getTextWidget().replaceTextRange(undoInfo.start, undoInfo.length, undoInfo.replacedText);
			}
			finally {
				doNotRecordUndoInfo = false;
			}
		}
	}
	
	private boolean canCutOrCopy() {
		boolean result = isEnabledAndEditable();
		
		if (result) {
			String text = getTextWidget().getSelectionText();
			
			result = text != null && text.length() > 0;
		}
		
		return result;
	}

	public boolean canCut() {
		return canCutOrCopy();
	}

	public void cut() {
		getTextWidget().cut();
	}
	
	public boolean canCopy() {
		return canCutOrCopy();
	}
	
	public void copy() {
		getTextWidget().copy();
	}
	
	public boolean canPaste() {
		String clipboardData;
		
		Clipboard clipboard = new Clipboard(Display.getCurrent());
		TextTransfer textTransfer = TextTransfer.getInstance();
		clipboardData = (String)clipboard.getContents(textTransfer);

		if (clipboardData == null) {
			RTFTransfer rtfTransfer = RTFTransfer.getInstance();
			clipboardData = (String)clipboard.getContents(rtfTransfer);
		}
		
		clipboard.dispose();

		return clipboardData != null;
	}
	
	public void paste() {
		getTextWidget().paste();
	}
	
	public boolean canSelectAll() {
		Point selectionRange = getSelectedRange();
		
		boolean allTextSelected = selectionRange.x == 0 && selectionRange.y == this.getTextWidget().getText().length();
		
		return isEnabledAndEditable() && !allTextSelected;
	}
	
	public void selectAll() {
		setSelection(new TextSelection(0, getTextWidget().getText().length()));
	}
	
	public boolean canFind() {
		return isEnabledAndEditable();
	}
	
	public void find(Shell shell) {
		if (canFind()) {
			if (Globals.getFindAndReplaceDialog() != null) {
				Globals.getFindAndReplaceDialog().close();
			}
			
			FindAndReplaceDialog findDialog = new FindAndReplaceDialog(shell, false);
			Globals.setFindAndReplaceDialog(findDialog);
			
			findDialog.open();
		}
	}
	
	public boolean canReplace() {
		return isEnabledAndEditable();
	}
	
	public void replace(Shell shell) {
		if (canReplace()) {
			if (Globals.getFindAndReplaceDialog() == null) {
				FindAndReplaceDialog findDialog = new FindAndReplaceDialog(shell, true);
				Globals.setFindAndReplaceDialog(findDialog);
			}
			else {
				Globals.getFindAndReplaceDialog().close();

				FindAndReplaceDialog findDialog = new FindAndReplaceDialog(shell, true);
				Globals.setFindAndReplaceDialog(findDialog);
			}
			
			Globals.getFindAndReplaceDialog().open();
		}
	}
	
	public boolean canSetFont() {
		return isEnabledAndEditable();
	}
	
	public void setFont() {
		FontDialog fontDialog = new FontDialog(getTextWidget().getShell());
		fontDialog.setText("Set Font");

		if (getOutlineItem().getRGB() != null) {
			fontDialog.setRGB(getOutlineItem().getRGB());
		}

		FontData[] fontList = FontUtils.stringToFontList(getFontString());
		
		if (fontList != null) {
			fontDialog.setFontList(fontList);
		}
		
		FontData fontData = fontDialog.open();
		
		if (fontData != null) {
			fontList = fontDialog.getFontList();
			
			String fontString = FontUtils.fontListToString(fontList);
			
			SWTFont font = new SWTFont(fontList[0].getName(), Globals.getPlatform(), fontString);
			
			OutlineItem outlineItem = getOutlineItem();
			
			if (outlineItem.getFontList() == null) {
				outlineItem.setFontList(new FontList());
			}
			
			String fontListBeforeFontAdded = FontList.serialize(outlineItem.getFontList());
			
			outlineItem.getFontList().add(font);
			
			String fontListAfterFontAdded = FontList.serialize(outlineItem.getFontList());
			
			// Only set the modified flag if the font list really changed.
			if (!fontListAfterFontAdded.equals(fontListBeforeFontAdded)) {
				Globals.getVaultDocument().setIsModified(true);
			}
			
			// rgb will be null for Ubuntu.
			RGB rgb = fontDialog.getRGB();
			
			boolean colorChanged = false;
			
			if (rgb != null && !rgb.equals(outlineItem.getRGB())) {
				getOutlineItem().setRGB(rgb);
				Globals.getVaultDocument().setIsModified(true);
				
				colorChanged = true;
			}

			setFontAndColor(colorChanged);
		}
	}
	
	@Override
	public void selectionChanged(SelectionChangedEvent event) {
    	IStructuredSelection selection = (IStructuredSelection) event.getSelection();

    	if (selection.size() != 1) {
    		setOutlineItem(null);
    		getTextWidget().setEnabled(false);
    	}
    	else {
        	OutlineItem outlineItem = (OutlineItem) selection.getFirstElement();
        	
        	setOutlineItem(outlineItem);
        	
    		getTextWidget().setEnabled(true);
    	}
    	
    	Globals.getMainApplicationWindow().getPhotoAndTextUI().setBackground(getTextWidget().getBackground());
    	
		getTextWidget().setVisible(getTextWidget().getEnabled());
	}

	public boolean canInsertTextFile() {
		return isEnabledAndEditable();
	}
	
	public void insertTextFile(Shell shell) {
		if (canInsertTextFile()) {
			FileDialog fileDialog = new FileDialog(shell, SWT.OPEN);
			fileDialog.setText("Insert Text File");
			fileDialog.setFilterExtensions(new String[] { StringLiterals.TextFileTypeWildcardedCaseInsensitive, StringLiterals.Wildcard });
			fileDialog.setFilterNames(new String[] { "Text Files", "All Files" });
			
			if (previousInsertTextFileFilterPath != null) {
				fileDialog.setFilterPath(previousInsertTextFileFilterPath);
			}
			
			boolean finished = false;
			
			do {
				String filePath = fileDialog.open();
				
				if (filePath != null && new File(filePath).exists()) {
					try {
						Globals.setBusyCursor();
						
						String text = FileUtils.readFile(filePath);
						getTextWidget().insert(text);
						
						previousInsertTextFileFilterPath = fileDialog.getFilterPath();
						
						finished = true;
					}
					catch (Throwable ex) {
						String message = MessageFormat.format("Cannot insert text file.{0}{0}{1}", PortabilityUtils.getNewLine(), ex.getMessage());
						
						Image icon = Globals.getImageRegistry().get(Globals.IMAGE_REGISTRY_VAULT_ICON);
						
						MessageDialog messageDialog = new MessageDialog(Globals.getMainApplicationWindow().getShell(), StringLiterals.ProgramName, icon, message, MessageDialog.ERROR, new String[] { "&OK" }, 0);
						messageDialog.open();
						
						ex.printStackTrace();
					}
					finally {
						Globals.setPreviousCursor();
					}
				}
				else if (filePath == null) {
					finished = true;
				}
			} while (!finished);
		}
	}

	public void highlightSearchHits() {
		if (searchPatterns != null && searchPatterns.length > 0) {
			String text = getTextWidget().getText();
			
			for (Pattern pattern : searchPatterns) {
				Matcher matcher = pattern.matcher(text);
				
				while (matcher.find()) {
					StyleRange styleRange = new StyleRange(matcher.start(), matcher.end() - matcher.start(), getTextWidget().getBackground(), getTextWidget().getForeground());
					getTextWidget().setStyleRange(styleRange);
				}
			}
		}
		else {
			// Clear existing highlights.
			StyleRange styleRange = new StyleRange(0, getTextWidget().getText().length(), getTextWidget().getForeground(), getTextWidget().getBackground());
			getTextWidget().setStyleRange(styleRange);
		}
	}
	
	/**
	 * Moves the cursor to the next or previous search and scrolls it into visibility. 
	 * @param next true if advancing to the next search hit, false if advancing to the previous search hit.
	 */
	private void nextPreviousSearchHit(boolean next) {
		if (searchPatterns.length > 0) {
			int caretOffset = getTextWidget().getCaretOffset();
			
			String text = getTextWidget().getText();

			for (Pattern pattern : searchPatterns) {
				Matcher matcher = pattern.matcher(text);
				
				while (matcher.find()) {
					if ((next && matcher.start() > caretOffset) || (!next && matcher.start() < caretOffset)) {
						getTextWidget().setFocus();
						getTextWidget().setCaretOffset(matcher.start());
						getTextWidget().showSelection();

						// If searching for the next search hit, jump out after the first one is found. 
						if (next) {
							break;
						}
					}
				}
			}
		}
	}

	private boolean canNextPreviousSearchHit() {
		return searchPatterns != null && searchPatterns.length > 0;
	}
	
	public boolean canNextSearchHit() {
		return canNextPreviousSearchHit();
	}
	
	public void nextSearchHit() {
		if (canNextSearchHit()) {
			nextPreviousSearchHit(true);
		}
	}
	
	public boolean canPreviousSearchHit() {
		return canNextPreviousSearchHit();
	}
	
	public void previousSearchHit() {
		if (canPreviousSearchHit()) {
			nextPreviousSearchHit(false);
		}
	}
	
	public class DocumentListener implements IDocumentListener {
		@Override
		public void documentAboutToBeChanged(DocumentEvent event) {
		}

		@Override
		public void documentChanged(DocumentEvent event) {
			Globals.getVaultDocument().setIsModified(true);
			isModified = true;
		}
	}
}
