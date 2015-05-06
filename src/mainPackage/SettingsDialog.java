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

import org.eclipse.jface.dialogs.IDialogConstants;
import org.eclipse.jface.preference.PreferenceStore;
import org.eclipse.swt.SWT;
import org.eclipse.swt.events.HelpEvent;
import org.eclipse.swt.events.HelpListener;
import org.eclipse.swt.events.ModifyEvent;
import org.eclipse.swt.events.ModifyListener;
import org.eclipse.swt.events.SelectionEvent;
import org.eclipse.swt.events.SelectionListener;
import org.eclipse.swt.events.VerifyEvent;
import org.eclipse.swt.events.VerifyListener;
import org.eclipse.swt.graphics.Color;
import org.eclipse.swt.graphics.FontData;
import org.eclipse.swt.graphics.Image;
import org.eclipse.swt.graphics.RGB;
import org.eclipse.swt.layout.GridData;
import org.eclipse.swt.layout.GridLayout;
import org.eclipse.swt.widgets.Button;
import org.eclipse.swt.widgets.Canvas;
import org.eclipse.swt.widgets.ColorDialog;
import org.eclipse.swt.widgets.Composite;
import org.eclipse.swt.widgets.Control;
import org.eclipse.swt.widgets.DirectoryDialog;
import org.eclipse.swt.widgets.Display;
import org.eclipse.swt.widgets.FileDialog;
import org.eclipse.swt.widgets.FontDialog;
import org.eclipse.swt.widgets.Label;
import org.eclipse.swt.widgets.Scale;
import org.eclipse.swt.widgets.Shell;
import org.eclipse.swt.widgets.TabFolder;
import org.eclipse.swt.widgets.TabItem;
import org.eclipse.swt.widgets.Text;

public class SettingsDialog extends VaultDialog {
	private PreferenceStore preferenceStore;
	
	@Override
	protected void configureShell(Shell newShell) {
		super.configureShell(newShell);
		
		newShell.setText("Settings");
	}

	@Override
	protected void populateFields() {
		slideshowExclusionText.setText(preferenceStore.getString(PreferenceKeys.SlideshowExclusions));
		photoEditingProgramText.setText(preferenceStore.getString(PreferenceKeys.PhotoEditingProgramPath));
		startupFilePathText.setText(preferenceStore.getString(PreferenceKeys.StarupFilePath));

		updateFontDisplay();
		
		substituteFolderLabel.setText(preferenceStore.getString(PreferenceKeys.SubstitutePhotoFolder));

		fromEmailAddressText.setText(preferenceStore.getString(PreferenceKeys.EmailFromAddress));
		emailUserNameText.setText(preferenceStore.getString(PreferenceKeys.EmailUserName));
		emailPasswordText.setText(preferenceStore.getString(PreferenceKeys.EmailPassword));
		emailServerAddressText.setText(preferenceStore.getString(PreferenceKeys.EmailServerAddress));
	}

	private char echoChar;
	
	private int autoSaveIntervalMinutes, checkForModificationsIntervalMinutes;

	private Button autoSaveCheckBox, saveWithBakFileTypeCheckBox, loadFileOnStartupButton, loadMostRecentlyUsedFileButton, 
				   doNotAutomaticallyLoadFileButton, loadPhotosFromOriginalLocationsRadioButton, loadPhotosFromSubstituteFolderRadioButton, okButton,
				   cachePasswords, allowMultipleInstances, authenticateEmail, advancedGraphics, slideShowFullScreen, checkForUpdatesCheckBox, warnAboutSingleInstance,
				   checkForModificationCheckBox, sslCheckbox;

	private Label substituteFolderLabel, defaultTextFontLabel, statusLabel;
	
	private Text startupFilePathText, fromEmailAddressText, emailServerAddressText, smtpPortText, emailUserNameText, emailPasswordText, slideshowExclusionText, 
	             photoEditingProgramText;
	
	private Canvas colorCanvas, textBackgroundColorCanvas;

	private Color nonErrorBackground, errorBackground;
	
	private String previousSubstitutePhotoFolder = null, fontString;

	private Button hidePasswordCharsCheckBox;

	private RGB fontColor;
	
	private RGB textBackgroundColor;
	
	@Override
	protected void createButtonsForButtonBar(Composite parent) {
		okButton = createButton(parent, IDialogConstants.OK_ID, "&OK", true);
		createButton(parent, IDialogConstants.CANCEL_ID, "&Cancel", false);
	}

	@Override
	protected Control createContents(Composite parent) {
		Control result = super.createContents(parent);
		
	    statusLabel = new Label(parent, SWT.BORDER);
	
		GridData gridData = new GridData(GridData.FILL_HORIZONTAL);
		statusLabel.setLayoutData(gridData);

		nonErrorBackground = statusLabel.getBackground();
		errorBackground = Display.getCurrent().getSystemColor(SWT.COLOR_RED);

		enableDisableOKButton();

		return result;
	}

	private void enableDisableOKButton() {
		boolean unspecifiedStartupFileError = loadFileOnStartupButton.getSelection() && startupFilePathText.getText().trim().length() == 0; 
		
		boolean unspecifiedSubstitutePhotoFolderError = (loadPhotosFromSubstituteFolderRadioButton.getSelection() && substituteFolderLabel.getText().trim().length() == 0);

		boolean authenticate = authenticateEmail.getSelection();
		
		okButton.setEnabled(!unspecifiedStartupFileError && !unspecifiedSubstitutePhotoFolderError);
		
	    if (unspecifiedStartupFileError) {
	    	statusLabel.setText("Startup File Must Be Specified in Startup File Tab.");
	        statusLabel.setBackground(errorBackground);
	    }
	    else if (unspecifiedSubstitutePhotoFolderError)
	    {
	    	statusLabel.setText("Substitute Photo Folder Must Be Specified in Photos Tab.");
	        statusLabel.setBackground(errorBackground);
	    }
	    else if (authenticate && emailUserNameText.getText().trim().length() == 0) {
	    	statusLabel.setText("User Name must be specified in Email Tab.");
	        statusLabel.setBackground(errorBackground);
	        okButton.setEnabled(false);
	    }
	    else if (authenticate && emailPasswordText.getText().trim().length() == 0) {
	    	statusLabel.setText("Password must be specified in Email Tab.");
	        statusLabel.setBackground(errorBackground);
	        okButton.setEnabled(false);
	    }
	    else {
	    	statusLabel.setText("");
	    	statusLabel.setBackground(nonErrorBackground);
	    }
	}
	
	@Override
	protected void okPressed() {
		preferenceStore.setValue(PreferenceKeys.LoadFileOnStartup,          loadFileOnStartupButton.getSelection()); 
		preferenceStore.setValue(PreferenceKeys.LoadMostRecentlyUsedFile,   loadMostRecentlyUsedFileButton.getSelection()); 
		preferenceStore.setValue(PreferenceKeys.DoNotAutomaticallyLoadFile, doNotAutomaticallyLoadFileButton.getSelection()); 
		preferenceStore.setValue(PreferenceKeys.StarupFilePath,             startupFilePathText.getText());
		
		preferenceStore.setValue(PreferenceKeys.AutoSaveMinutes, autoSaveCheckBox.getSelection() ? autoSaveIntervalMinutes : 0);
		preferenceStore.setValue(PreferenceKeys.SaveOldFileWithBakType, saveWithBakFileTypeCheckBox.getSelection());
		
		preferenceStore.setValue(PreferenceKeys.CheckForModificationsMinutes, checkForModificationCheckBox.getSelection() ? checkForModificationsIntervalMinutes : 0);
		
		preferenceStore.setValue(PreferenceKeys.LoadPhotosFromOriginalLocations, loadPhotosFromOriginalLocationsRadioButton.getSelection());
		preferenceStore.setValue(PreferenceKeys.SubstitutePhotoFolder, substituteFolderLabel.getText());
		preferenceStore.setValue(PreferenceKeys.AdvancedGraphics, advancedGraphics.getSelection());
		preferenceStore.setValue(PreferenceKeys.SlideshowFullScreen, slideShowFullScreen.getSelection());
		preferenceStore.setValue(PreferenceKeys.SlideshowExclusions, slideshowExclusionText.getText());
		
		preferenceStore.setValue(PreferenceKeys.DefaultTextFont, fontString);
		
		preferenceStore.setValue(PreferenceKeys.DefaultTextFontRed, fontColor.red);
		preferenceStore.setValue(PreferenceKeys.DefaultTextFontGreen, fontColor.green);
		preferenceStore.setValue(PreferenceKeys.DefaultTextFontBlue, fontColor.blue);
		
		preferenceStore.setValue(PreferenceKeys.TextBackgroundRed, textBackgroundColor.red);
		preferenceStore.setValue(PreferenceKeys.TextBackgroundGreen, textBackgroundColor.green);
		preferenceStore.setValue(PreferenceKeys.TextBackgroundBlue, textBackgroundColor.blue);
		
		Globals.getVaultTextViewer().setBackgroundColor();
		
		preferenceStore.setValue(PreferenceKeys.CachePasswords, cachePasswords.getSelection());
		
		preferenceStore.setValue(PreferenceKeys.AllowMultipleInstances, allowMultipleInstances.getSelection());

		if (warnAboutSingleInstance.isEnabled()) {
			preferenceStore.setValue(PreferenceKeys.WarnAboutSingleInstance, warnAboutSingleInstance.getSelection());
		}
		
		preferenceStore.setValue(PreferenceKeys.EmailFromAddress, fromEmailAddressText.getText());
		preferenceStore.setValue(PreferenceKeys.EmailServerAddress, emailServerAddressText.getText());
		preferenceStore.setValue(PreferenceKeys.EmailSMTPPort, smtpPortText.getText());

		preferenceStore.setValue(PreferenceKeys.EmailUserName, emailUserNameText.getText());
		preferenceStore.setValue(PreferenceKeys.EmailPassword, emailPasswordText.getText());
		preferenceStore.setValue(PreferenceKeys.EmailAuthentication, authenticateEmail.getSelection());
		preferenceStore.setValue(PreferenceKeys.EmailSSL, sslCheckbox.getSelection());
		
		preferenceStore.setValue(PreferenceKeys.HidePasswordCharacters, hidePasswordCharsCheckBox.getSelection());
		
		preferenceStore.setValue(PreferenceKeys.PhotoEditingProgramPath, photoEditingProgramText.getText());
		
		preferenceStore.setValue(PreferenceKeys.CheckForUpdatesAutomatically, checkForUpdatesCheckBox.getSelection());
		
		super.okPressed();
	}

	@Override
	protected Control createDialogArea(Composite parent) {
		Composite composite = (Composite) super.createDialogArea(parent);
		composite.setLayout(new GridLayout());

		TabFolder tabFolder = new TabFolder(composite, SWT.NONE);
		GridData gridData = new GridData(GridData.FILL_BOTH);
		tabFolder.setLayoutData(gridData);
		
		TabItem startupFileTabItem = new TabItem(tabFolder, SWT.NONE);
		startupFileTabItem.setText("&Startup File");

		TabItem savingTabItem = new TabItem(tabFolder, SWT.NONE);
		savingTabItem.setText("Sa&ving");
		
		TabItem syncTabItem = new TabItem(tabFolder, SWT.NONE);
		syncTabItem.setText("S&ync");
		
		TabItem passwordsTabItem = new TabItem(tabFolder, SWT.NONE);
		passwordsTabItem.setText("P&asswords");

		TabItem instancesTabItem = new TabItem(tabFolder, SWT.NONE);
		instancesTabItem.setText("&Instances");
		
		TabItem defaultTextFontTabItem = new TabItem(tabFolder, SWT.NONE);
		defaultTextFontTabItem.setText("Default Te&xt Font");
		
		TabItem photosTabItem = new TabItem(tabFolder, SWT.NONE);
		photosTabItem.setText("&Photos && Slideshows");
		
		TabItem substituteFolderTabItem = new TabItem(tabFolder, SWT.NONE);
		substituteFolderTabItem.setText("Su&bstitute Folder");
		
		TabItem emailTabItem = new TabItem(tabFolder, SWT.NONE);
		emailTabItem.setText("&Email");
		
		TabItem updatesTabItem = new TabItem(tabFolder, SWT.NONE);
		updatesTabItem.setText("&Updates");

		Composite startupComposite = new Composite(tabFolder, SWT.NONE);
		GridLayout gridLayout = new GridLayout(1, false);
		startupComposite.setLayout(gridLayout);
		
		loadFileOnStartupButton = new Button(startupComposite, SWT.RADIO);
		loadFileOnStartupButton.setText("Open the Fo&llowing File on Startup:");

		loadFileOnStartupButton.addSelectionListener(new SelectionListener() {
			@Override
			public void widgetDefaultSelected(SelectionEvent e) {
			}

			@Override
			public void widgetSelected(SelectionEvent e) {
				enableDisableOKButton();
			}
		});
		
		startupFilePathText = new Text(startupComposite, SWT.BORDER);
		gridData = new GridData(GridData.FILL_HORIZONTAL);
		gridData.grabExcessHorizontalSpace = true;
		startupFilePathText.setLayoutData(gridData);
		
		startupFilePathText.addModifyListener(new ModifyListener() {
			@Override
			public void modifyText(ModifyEvent e) {
				enableDisableOKButton();
			}
		});

		startupFilePathText.addFocusListener(new TextFocusListener());
		
		new Label(startupComposite, SWT.NONE).setText("");

		Button specifyFileButton = new Button(startupComposite, SWT.NONE);
		specifyFileButton.setText("Specify Startup &File...");
		
		specifyFileButton.addSelectionListener(new SelectionListener() {
			@Override
			public void widgetDefaultSelected(SelectionEvent e) {
			}

			@Override
			public void widgetSelected(SelectionEvent e) {
				FileDialog fileDialog = new FileDialog(getShell(), SWT.OPEN);
				String vault3File = MessageFormat.format("{0} File", StringLiterals.ProgramName);
				fileDialog.setFilterNames(new String[] { vault3File, "All Files" });
				fileDialog.setFilterExtensions(new String[] { StringLiterals.ProgramFileTypeWildcardedCaseInsensitive, StringLiterals.Wildcard });
				fileDialog.setText("Specify File");
				
				boolean finished = false;
				
				do {
					String filePath = fileDialog.open();
					
					if (filePath != null && new File(filePath).exists()) {
						loadFileOnStartupButton.setSelection(true);
						loadMostRecentlyUsedFileButton.setSelection(false);
						doNotAutomaticallyLoadFileButton.setSelection(false);
						startupFilePathText.setText(filePath);
						
						finished = true;
					}
					else if (filePath == null) { 
						finished = true;
					}
				} while (!finished);
				
				enableDisableOKButton();
			}
		});
		
		new Label(startupComposite, SWT.NONE).setText("");
		
		loadMostRecentlyUsedFileButton = new Button(startupComposite, SWT.RADIO);
		loadMostRecentlyUsedFileButton.setText("Open &Most Recently Opened File on Startup");
		
		loadMostRecentlyUsedFileButton.addSelectionListener(new SelectionListener() {
			@Override
			public void widgetDefaultSelected(SelectionEvent e) {
			}

			@Override
			public void widgetSelected(SelectionEvent e) {
				enableDisableOKButton();
			}
		});

		doNotAutomaticallyLoadFileButton = new Button(startupComposite, SWT.RADIO);
		doNotAutomaticallyLoadFileButton.setText("Don't &Automatically Open File on Startup");
		
		doNotAutomaticallyLoadFileButton.addSelectionListener(new SelectionListener() {
			@Override
			public void widgetDefaultSelected(SelectionEvent e) {
			}

			@Override
			public void widgetSelected(SelectionEvent e) {
				enableDisableOKButton();
			}
		});

		loadFileOnStartupButton.setSelection(preferenceStore.getBoolean(PreferenceKeys.LoadFileOnStartup));
		loadMostRecentlyUsedFileButton.setSelection(preferenceStore.getBoolean(PreferenceKeys.LoadMostRecentlyUsedFile));
		doNotAutomaticallyLoadFileButton.setSelection(preferenceStore.getBoolean(PreferenceKeys.DoNotAutomaticallyLoadFile));
		
		if (!loadFileOnStartupButton.getSelection() && !loadMostRecentlyUsedFileButton.getSelection() && !doNotAutomaticallyLoadFileButton.getSelection()) {
			doNotAutomaticallyLoadFileButton.setSelection(true);
		}
		
		final Composite savingComposite = new Composite(tabFolder, SWT.NONE);
		gridLayout = new GridLayout(3, false);
		savingComposite.setLayout(gridLayout);

		autoSaveCheckBox = new Button(savingComposite, SWT.CHECK);
		autoSaveCheckBox.setText("A&utomatically Save Document Every");
		
		final Scale saveIntervalScale = new Scale(savingComposite, SWT.HORIZONTAL);
		saveIntervalScale.setMinimum(1);
		saveIntervalScale.setMaximum(60);
		
		int minutes = preferenceStore.getInt(PreferenceKeys.AutoSaveMinutes);
		
		autoSaveCheckBox.setSelection(minutes > 0);
		
		if (minutes > 0) {
			saveIntervalScale.setSelection(minutes);
		}

		final String minutesFormat = "{0} Minute(s)";
		
		final Label autoSaveIntervalLabel = new Label(savingComposite, SWT.NONE);
		String text = MessageFormat.format(minutesFormat, saveIntervalScale.getSelection());
		autoSaveIntervalLabel.setText(text);
		
		autoSaveIntervalMinutes = saveIntervalScale.getSelection();

		// Allocate room for 2 digits (plus some extra space to account for different width of digits).
		gridData = new GridData(GridData.FILL_HORIZONTAL);
		gridData.minimumWidth = GraphicsUtils.getTextExtent(text).x + GraphicsUtils.getTextExtent("0").x * 3;
		gridData.horizontalAlignment = SWT.LEFT;
		autoSaveIntervalLabel.setLayoutData(gridData);

		saveIntervalScale.addSelectionListener(new SelectionListener() {
			@Override
			public void widgetDefaultSelected(SelectionEvent e) {
			}

			@Override
			public void widgetSelected(SelectionEvent e) {
				String text = MessageFormat.format(minutesFormat, saveIntervalScale.getSelection());
				autoSaveIntervalLabel.setText(text);
				autoSaveIntervalMinutes = saveIntervalScale.getSelection();
			}
		});
		
		saveWithBakFileTypeCheckBox = new Button(savingComposite, SWT.CHECK);
		saveWithBakFileTypeCheckBox.setText("&Before Saving a File, Save the Old File as <FILENAME>.bak");
		
		saveWithBakFileTypeCheckBox.setSelection(preferenceStore.getBoolean(PreferenceKeys.SaveOldFileWithBakType));
		
		gridData = new GridData();
		gridData.horizontalSpan = gridLayout.numColumns;
		saveWithBakFileTypeCheckBox.setLayoutData(gridData);

		final Composite syncComposite = new Composite(tabFolder, SWT.NONE);
		gridLayout = new GridLayout(3, false);
		syncComposite.setLayout(gridLayout);

		checkForModificationCheckBox = new Button(syncComposite, SWT.CHECK);
		checkForModificationCheckBox.setText("Check for Document Changes Every");
		
		final Scale checkForModificationsIntervalScale = new Scale(syncComposite, SWT.HORIZONTAL);
		checkForModificationsIntervalScale.setMinimum(1);
		checkForModificationsIntervalScale.setMaximum(60);
		
		minutes = preferenceStore.getInt(PreferenceKeys.CheckForModificationsMinutes);
		
		checkForModificationCheckBox.setSelection(minutes > 0);
		
		if (minutes > 0) {
			checkForModificationsIntervalScale.setSelection(minutes);
		}

		final Label checkForModificationsIntervalLabel = new Label(syncComposite, SWT.NONE);
		text = MessageFormat.format(minutesFormat, checkForModificationsIntervalScale.getSelection());
		checkForModificationsIntervalLabel.setText(text);
		
		checkForModificationsIntervalMinutes = checkForModificationsIntervalScale.getSelection();

		// Allocate room for 2 digits (plus some extra space to account for different width of digits).
		gridData = new GridData(GridData.FILL_HORIZONTAL);
		gridData.minimumWidth = GraphicsUtils.getTextExtent(text).x + GraphicsUtils.getTextExtent("0").x * 3;
		gridData.horizontalAlignment = SWT.LEFT;
		checkForModificationsIntervalLabel.setLayoutData(gridData);

		checkForModificationsIntervalScale.addSelectionListener(new SelectionListener() {
			@Override
			public void widgetDefaultSelected(SelectionEvent e) {
			}

			@Override
			public void widgetSelected(SelectionEvent e) {
				String text = MessageFormat.format(minutesFormat, checkForModificationsIntervalScale.getSelection());
				checkForModificationsIntervalLabel.setText(text);
				checkForModificationsIntervalMinutes = checkForModificationsIntervalScale.getSelection();
			}
		});
		
		
		final Composite passwordsComposite = new Composite(tabFolder, SWT.NONE);
		gridLayout = new GridLayout(1, false);
		passwordsComposite.setLayout(gridLayout);
		
		cachePasswords = new Button(passwordsComposite, SWT.CHECK | SWT.WRAP);
		cachePasswords.setText(MessageFormat.format("Onl&y require a password to be entered the first time a given {0} document is accessed after {0} is launched", StringLiterals.ProgramName));
		cachePasswords.setSelection(preferenceStore.getBoolean(PreferenceKeys.CachePasswords));
		
		final Composite instancesComposite = new Composite(tabFolder, SWT.NONE);
		gridLayout = new GridLayout(1, false);
		instancesComposite.setLayout(gridLayout);
		
		Composite twoItemsComposite = new Composite(instancesComposite, SWT.NONE);
		gridLayout = new GridLayout(2, false);
		gridLayout.marginWidth = 0;
		twoItemsComposite.setLayout(gridLayout);
		
		allowMultipleInstances = new Button(twoItemsComposite, SWT.CHECK);
		allowMultipleInstances.setText(MessageFormat.format("&Allow multiple instances of {0}", StringLiterals.ProgramName));
		allowMultipleInstances.setSelection(preferenceStore.getBoolean(PreferenceKeys.AllowMultipleInstances));
		
		Image image = Globals.getImageRegistry().get(Globals.IMAGE_REGISTRY_LIGHTBULB);
		
		Label imageLabel = new Label(twoItemsComposite, SWT.NONE);
		imageLabel.setImage(image);
		
		imageLabel.setToolTipText(MessageFormat.format("This setting will take effect after all instances of {0} are shut down.", StringLiterals.ProgramName));
		
		warnAboutSingleInstance = new Button(instancesComposite, SWT.CHECK);
		warnAboutSingleInstance.setText(MessageFormat.format("&Warn when switching to running instance of {0}", StringLiterals.ProgramName));
		warnAboutSingleInstance.setSelection(preferenceStore.getBoolean(PreferenceKeys.WarnAboutSingleInstance));

		allowMultipleInstances.addSelectionListener(new SelectionListener() {
			@Override
			public void widgetSelected(SelectionEvent e) {
				enableDisableWarnAboutSingleInstance();
			}
			
			@Override
			public void widgetDefaultSelected(SelectionEvent e) {
			}
		});
		
		enableDisableWarnAboutSingleInstance();

		Composite defaultTextFontComposite = new Composite(tabFolder, SWT.NONE);
		gridLayout = new GridLayout(2, false);
		defaultTextFontComposite.setLayout(gridLayout);
		
		defaultTextFontLabel = new Label(defaultTextFontComposite, SWT.NONE);
		gridData = new GridData(GridData.FILL_HORIZONTAL);
		gridData.horizontalSpan = 2;
		defaultTextFontLabel.setLayoutData(gridData);

		Label defaultTextFontColorLabel = new Label(defaultTextFontComposite, SWT.NONE);
		defaultTextFontColorLabel.setText("Color:");

		colorCanvas = new Canvas(defaultTextFontComposite, SWT.BORDER);
		colorCanvas.setBackground(Globals.getColorRegistry().get(fontColor));
		
		gridData = new GridData();
		gridData.heightHint = gridData.widthHint = GraphicsUtils.getTextExtent(defaultTextFontColorLabel.getText()).y;
		colorCanvas.setLayoutData(gridData);

		Label spacerLabel = new Label(defaultTextFontComposite, SWT.NONE);
		gridData = new GridData();
		gridData.horizontalSpan = 2;
		spacerLabel.setLayoutData(gridData);

		Button specifyFontButton = new Button(defaultTextFontComposite, SWT.NONE);
		specifyFontButton.setText("Specify &Font...");
		
		specifyFontButton.addSelectionListener(new SelectionListener() {
			@Override
			public void widgetDefaultSelected(SelectionEvent e) {
			}

			@Override
			public void widgetSelected(SelectionEvent e) {
				FontDialog fontDialog = new FontDialog(getShell());

				FontData[] fontList = FontUtils.stringToFontList(preferenceStore.getString(PreferenceKeys.DefaultTextFont));
				
				if (fontList != null) {
					fontDialog.setFontList(fontList);
				}
				
				int red   = preferenceStore.getInt(PreferenceKeys.DefaultTextFontRed);
				int green = preferenceStore.getInt(PreferenceKeys.DefaultTextFontGreen);
				int blue  = preferenceStore.getInt(PreferenceKeys.DefaultTextFontBlue);
				fontDialog.setRGB(new RGB(red, green, blue));
				
				fontDialog.setText("Specify Font");
				
				FontData fontData = fontDialog.open();
				
				if (fontData != null) {
					fontList = fontDialog.getFontList();
					
					fontString = FontUtils.fontListToString(fontList);
					fontColor = fontDialog.getRGB();
					
					updateFontDisplay();
				}
			}
		});

		spacerLabel = new Label(defaultTextFontComposite, SWT.NONE);
		gridData = new GridData();
		gridData.horizontalSpan = 2;
		spacerLabel.setLayoutData(gridData);

		Label textBackgroundColorLabel = new Label(defaultTextFontComposite, SWT.NONE);
		textBackgroundColorLabel.setText("Text Background Color:");

		textBackgroundColorCanvas = new Canvas(defaultTextFontComposite, SWT.BORDER);
		textBackgroundColorCanvas.setBackground(Globals.getColorRegistry().get(textBackgroundColor));

		gridData = new GridData();
		gridData.heightHint = gridData.widthHint = GraphicsUtils.getTextExtent(textBackgroundColorLabel.getText()).y;
		textBackgroundColorCanvas.setLayoutData(gridData);

		Button specifyBackgroundColorButton = new Button(defaultTextFontComposite, SWT.NONE);
		specifyBackgroundColorButton.setText("Specify &Text Background Color...");
		
		specifyBackgroundColorButton.addSelectionListener(new SelectionListener() {
			@Override
			public void widgetDefaultSelected(SelectionEvent arg0) {
			}

			@Override
			public void widgetSelected(SelectionEvent arg0) {
				ColorDialog colorDialog = new ColorDialog(getShell());
				colorDialog.setText("Text Background Color");
				colorDialog.setRGB(textBackgroundColor);
				
				RGB newColor = colorDialog.open();
				
				if (newColor != null) {
					textBackgroundColor = newColor;
					textBackgroundColorCanvas.setBackground(Globals.getColorRegistry().get(textBackgroundColor));
				}
			}
		});
		
		Composite substituteFolderComposite = new Composite(tabFolder, SWT.NONE);
		gridLayout = new GridLayout(1, false);
		substituteFolderComposite.setLayout(gridLayout);
		
		loadPhotosFromOriginalLocationsRadioButton = new Button(substituteFolderComposite, SWT.RADIO);
		loadPhotosFromOriginalLocationsRadioButton.setText("&Load Photos and file:/// URLs from Original Locations");
		
		loadPhotosFromSubstituteFolderRadioButton = new Button(substituteFolderComposite, SWT.RADIO);
		loadPhotosFromSubstituteFolderRadioButton.setText("Loa&d Photos and file:/// URLs from Substitute Folder");
		
		loadPhotosFromOriginalLocationsRadioButton.addSelectionListener(new SelectionListener() {
			@Override
			public void widgetDefaultSelected(SelectionEvent e) {
			}

			@Override
			public void widgetSelected(SelectionEvent e) {
				enableDisableOKButton();
			}
		});
		
		loadPhotosFromSubstituteFolderRadioButton.addSelectionListener(new SelectionListener() {
			@Override
			public void widgetDefaultSelected(SelectionEvent e) {
			}

			@Override
			public void widgetSelected(SelectionEvent e) {
				enableDisableOKButton();
			}
		});
		
		final boolean loadFromOriginal = preferenceStore.getBoolean(PreferenceKeys.LoadPhotosFromOriginalLocations);
		
		loadPhotosFromOriginalLocationsRadioButton.setSelection(loadFromOriginal);
		loadPhotosFromSubstituteFolderRadioButton.setSelection(!loadFromOriginal);
		
		substituteFolderLabel = new Label(substituteFolderComposite, SWT.NONE);
		gridData = new GridData(GridData.FILL_HORIZONTAL);
		substituteFolderLabel.setLayoutData(gridData);
		
		// Spacer.
		new Label(substituteFolderComposite, SWT.NONE).setText("");
		
		Button specifyFolderButton = new Button(substituteFolderComposite, SWT.NONE);
		specifyFolderButton.setText("Specify &Folder...");
		gridData = new GridData();
		gridData.horizontalAlignment = SWT.LEFT;
		specifyFolderButton.setLayoutData(gridData);
		
		specifyFolderButton.addSelectionListener(new SelectionListener() {
			@Override
			public void widgetDefaultSelected(SelectionEvent e) {
			}

			@Override
			public void widgetSelected(SelectionEvent e) {
				DirectoryDialog directoryDialog = new DirectoryDialog(getShell());
				directoryDialog.setText("Specify Substitute Photos Folder");
				directoryDialog.setMessage("Folder:");
				directoryDialog.setFilterPath(previousSubstitutePhotoFolder);
				
				String substituteFolder = directoryDialog.open();
				
				if (substituteFolder != null) {
					substituteFolderLabel.setText(substituteFolder);
					loadPhotosFromOriginalLocationsRadioButton.setSelection(false);
					loadPhotosFromSubstituteFolderRadioButton.setSelection(true);
					previousSubstitutePhotoFolder = substituteFolder;
					substituteFolderLabel.setText(substituteFolder);
					
					enableDisableOKButton();
				}
			}
		});

		Composite photosComposite = new Composite(tabFolder, SWT.NONE);
		gridLayout = new GridLayout(1, false);
		photosComposite.setLayout(gridLayout);
		
		Composite advancedGraphicsComposite = new Composite(photosComposite, SWT.NONE);
		advancedGraphicsComposite.setLayout(new GridLayout(2, false));
		
		advancedGraphics = new Button(advancedGraphicsComposite, SWT.CHECK);
		advancedGraphics.setText("&Use Advanced Graphics");
		advancedGraphics.setSelection(preferenceStore.getBoolean(PreferenceKeys.AdvancedGraphics));
		
		image = Globals.getImageRegistry().get(Globals.IMAGE_REGISTRY_LIGHTBULB);
		
		imageLabel = new Label(advancedGraphicsComposite, SWT.NONE);
		imageLabel.setImage(image);
		
		imageLabel.setToolTipText("Disable advanced graphics if photos are rendered slowly with low quality.");

		Composite slideShowFullScreenComposite = new Composite(photosComposite, SWT.NONE);
		slideShowFullScreenComposite.setLayout(new GridLayout(2, false));
		
		slideShowFullScreen = new Button(advancedGraphicsComposite, SWT.CHECK);
		slideShowFullScreen.setText("&Run Slideshows in Full Screen Mode");
		slideShowFullScreen.setSelection(preferenceStore.getBoolean(PreferenceKeys.SlideshowFullScreen));
				
		Composite exclusionsComposite = new Composite(photosComposite, SWT.NONE);
		exclusionsComposite.setLayout(new GridLayout(3, false));
		
		gridData = new GridData(GridData.FILL_HORIZONTAL);
		gridData.grabExcessHorizontalSpace = true;
		exclusionsComposite.setLayoutData(gridData);
		
		Label exclusionsLabel = new Label(exclusionsComposite, SWT.NONE);
		exclusionsLabel.setText("In Slideshows and Photo Exports, Excl&ude Items Containing:");
		
		slideshowExclusionText = new Text(exclusionsComposite, SWT.BORDER); 
		
		gridData = new GridData(GridData.FILL_HORIZONTAL);
		gridData.grabExcessHorizontalSpace = true;
		slideshowExclusionText.setLayoutData(gridData);
		
		// Spacer.
		new Label(photosComposite, SWT.NONE).setText("");

		Button specifyPhotoEditingProgramButton = new Button(photosComposite, SWT.NONE);
		specifyPhotoEditingProgramButton.setText("Specify Photo Editing &Program...");
				
		photoEditingProgramText = new Text(photosComposite, SWT.BORDER);

		gridData = new GridData(GridData.FILL_HORIZONTAL);
		gridData.grabExcessHorizontalSpace = true;
		photoEditingProgramText.setLayoutData(gridData);
		
		specifyPhotoEditingProgramButton.addSelectionListener(new SelectionListener() {
			@Override
			public void widgetDefaultSelected(SelectionEvent e) {
			}

			@Override
			public void widgetSelected(SelectionEvent e) {
				FileDialog fileDialog = new FileDialog(getShell(), SWT.OPEN);
				fileDialog.setFilterNames(new String[] { "Executable Files", "All Files" });
				fileDialog.setFilterExtensions(new String[] { StringLiterals.ExecutableFileType, StringLiterals.Wildcard });
				fileDialog.setText("Specify Photo Editing Program");
				
				boolean finished = false;
				
				do {
					String filePath = fileDialog.open();
					
					if (filePath != null && new File(filePath).exists()) {
						photoEditingProgramText.setText(filePath);
						
						finished = true;
					}
					else if (filePath == null) { 
						finished = true;
					}
				} while (!finished);
			}
		});

		imageLabel = new Label(exclusionsComposite, SWT.NONE);
		imageLabel.setImage(image);
		
		imageLabel.setToolTipText(StringLiterals.SearchTextToolTip);
		
		Composite emailComposite = new Composite(tabFolder, SWT.NONE);
		emailComposite.setLayout(new GridLayout(2, false));
		
		Label fromEmailAddressLabel = new Label(emailComposite, SWT.NONE);
		fromEmailAddressLabel.setText("&From Email Address (Your Email Address):");
		
		fromEmailAddressText = new Text(emailComposite, SWT.BORDER);
		gridData = new GridData();
		gridData.grabExcessHorizontalSpace = true;
		gridData.horizontalAlignment = SWT.FILL;
		fromEmailAddressText.setLayoutData(gridData);
		
		fromEmailAddressText.addFocusListener(new TextFocusListener());
		
		sslCheckbox = new Button(emailComposite, SWT.CHECK);
		sslCheckbox.setText("SS&L");
		sslCheckbox.setSelection(preferenceStore.getBoolean(PreferenceKeys.EmailSSL));
		gridData = new GridData();
		gridData.horizontalSpan = 2;
		sslCheckbox.setLayoutData(gridData);
		
		authenticateEmail = new Button(emailComposite, SWT.CHECK);
		authenticateEmail.setText("A&uthenticate");
		authenticateEmail.setSelection(preferenceStore.getBoolean(PreferenceKeys.EmailAuthentication));
		gridData = new GridData();
		gridData.horizontalSpan = 2;
		authenticateEmail.setLayoutData(gridData);
		
		authenticateEmail.addSelectionListener(new SelectionListener() {
			@Override
			public void widgetDefaultSelected(SelectionEvent e) {
			}

			@Override
			public void widgetSelected(SelectionEvent e) {
				enableDisableEmailCredentials();
				enableDisableOKButton();
			}
		});
		
		Label emailUserNameLabel = new Label(emailComposite, SWT.NONE);
		emailUserNameLabel.setText("&User Name:");
		
		emailUserNameText = new Text(emailComposite, SWT.BORDER);
		gridData = new GridData();
		gridData.grabExcessHorizontalSpace = true;
		gridData.horizontalAlignment = SWT.FILL;
		emailUserNameText.setLayoutData(gridData);
		
		emailUserNameText.addModifyListener(new ModifyListener() {
			@Override
			public void modifyText(ModifyEvent e) {
				enableDisableOKButton();
			}
		});
		
		emailUserNameText.addFocusListener(new TextFocusListener());
		
		Label emailPasswordLabel = new Label(emailComposite, SWT.NONE);
		emailPasswordLabel.setText("&Password:");
		
		emailPasswordText = new Text(emailComposite, SWT.PASSWORD | SWT.BORDER);
		gridData = new GridData();
		gridData.grabExcessHorizontalSpace = true;
		gridData.horizontalAlignment = SWT.FILL;
		emailPasswordText.setLayoutData(gridData);
		emailPasswordText.addFocusListener(new TextFocusListener());
		
		emailPasswordText.addModifyListener(new ModifyListener() {
			@Override
			public void modifyText(ModifyEvent e) {
				enableDisableOKButton();
			}
		});
		
		echoChar = emailPasswordText.getEchoChar();
		
		hidePasswordCharsCheckBox = new Button(emailComposite, SWT.CHECK);
		hidePasswordCharsCheckBox.setText("&Hide password characters");
		gridData = new GridData();
		gridData.horizontalSpan = 2;
		hidePasswordCharsCheckBox.setLayoutData(gridData);
		
		hidePasswordCharsCheckBox.setSelection(preferenceStore.getBoolean(PreferenceKeys.HidePasswordCharacters));
		
		hidePasswordCharsCheckBox.addSelectionListener(new SelectionListener() {
			@Override
			public void widgetDefaultSelected(SelectionEvent e) {
			}

			@Override
			public void widgetSelected(SelectionEvent e) {
				showHidePasswordCharacters();
			}
		});
		
		showHidePasswordCharacters();

		enableDisableEmailCredentials();
		
		Label emailServerAddressLabel = new Label(emailComposite, SWT.NONE);
		emailServerAddressLabel.setText("&Email Server Address (e.g. smtp.example.com):");
		
		emailServerAddressText = new Text(emailComposite, SWT.BORDER);
		
		Label smtpPortLabel = new Label(emailComposite, SWT.NONE);
		smtpPortLabel.setText("Po&rt:");

		smtpPortText = new Text(emailComposite, SWT.BORDER);
		smtpPortText.setText(preferenceStore.getString(PreferenceKeys.EmailSMTPPort));

		// Restrict port to digits.
		smtpPortText.addVerifyListener(new VerifyListener() {
			@Override
			public void verifyText(VerifyEvent verifyEvent) {
		        switch (verifyEvent.keyCode) {  
	            case SWT.BS:           // Backspace  
	            case SWT.DEL:          // Delete  
	            case SWT.HOME:         // Home  
	            case SWT.END:          // End  
	            case SWT.ARROW_LEFT:   // Left arrow  
	            case SWT.ARROW_RIGHT:  // Right arrow
	            	return;
	            	
	            case 0:				   // Paste
	            	{
	            		if (verifyEvent.text != null) {
	            			for (int i = 0; i < verifyEvent.text.length(); i++) {
	            				if (!Character.isDigit(verifyEvent.text.charAt(i))) {
	            					verifyEvent.doit = false;
	            					break;
	            				}
	            			}
	            		}
	            		
	            		return;
	            	}
		        }  
	  
		        if (!Character.isDigit(verifyEvent.character)) {  
		            verifyEvent.doit = false;  // disallow the action  
		        }  
	        }
		});
		
		gridData = new GridData();
		gridData.grabExcessHorizontalSpace = true;
		gridData.horizontalAlignment = SWT.FILL;
		emailServerAddressText.setLayoutData(gridData);
		emailServerAddressText.addFocusListener(new TextFocusListener());
		
		smtpPortText.setLayoutData(gridData);

		Composite updatesComposite = new Composite(tabFolder, SWT.NONE);
		updatesComposite.setLayout(new GridLayout(1, false));
		
		checkForUpdatesCheckBox = new Button(updatesComposite, SWT.CHECK);
		checkForUpdatesCheckBox.setText("&Check for updates once a week");
		checkForUpdatesCheckBox.setSelection(preferenceStore.getBoolean(PreferenceKeys.CheckForUpdatesAutomatically));
		
		startupFileTabItem.setControl(startupComposite);
		savingTabItem.setControl(savingComposite);
		syncTabItem.setControl(syncComposite);
		passwordsTabItem.setControl(passwordsComposite);
		instancesTabItem.setControl(instancesComposite);
		defaultTextFontTabItem.setControl(defaultTextFontComposite);
		photosTabItem.setControl(photosComposite);
		substituteFolderTabItem.setControl(substituteFolderComposite);
		emailTabItem.setControl(emailComposite);
		updatesTabItem.setControl(updatesComposite);
		
		parent.addHelpListener(new HelpListener() {
			@Override
			public void helpRequested(HelpEvent e) {
				HelpUtils.ProcessHelpRequest("Dialogs_SettingsDialog");
			}
		});
		
		composite.pack();

		return composite;
	}

	private void enableDisableWarnAboutSingleInstance() {
		warnAboutSingleInstance.setEnabled(!allowMultipleInstances.getSelection());
	}
	
	private void enableDisableEmailCredentials() {
		boolean enabled = authenticateEmail.getSelection();
		
		emailUserNameText.setEnabled(enabled);
		emailPasswordText.setEnabled(enabled);
	}
	
	private void showHidePasswordCharacters() {
		if (hidePasswordCharsCheckBox.getSelection()) {
			emailPasswordText.setEchoChar(echoChar);
		}
		else {
			emailPasswordText.setEchoChar('\0');
		}
	}

	private void updateFontDisplay() {
		String defaultTextFontLabelText = MessageFormat.format("Default Text Font: {0}", FontUtils.stringToDescription(fontString));
		defaultTextFontLabel.setText(defaultTextFontLabelText);
		
		colorCanvas.setBackground(Globals.getColorRegistry().get(fontColor));
	}
	
	public SettingsDialog(Shell parentShell) {
		super(parentShell);
		
		preferenceStore = Globals.getPreferenceStore();
		
		fontColor = new RGB(preferenceStore.getInt(PreferenceKeys.DefaultTextFontRed),
							preferenceStore.getInt(PreferenceKeys.DefaultTextFontGreen),
							preferenceStore.getInt(PreferenceKeys.DefaultTextFontBlue));

		textBackgroundColor = new RGB(preferenceStore.getInt(PreferenceKeys.TextBackgroundRed),
									  preferenceStore.getInt(PreferenceKeys.TextBackgroundGreen),
									  preferenceStore.getInt(PreferenceKeys.TextBackgroundBlue));

		fontString = preferenceStore.getString(PreferenceKeys.DefaultTextFont);
		
		previousSubstitutePhotoFolder = preferenceStore.getString(PreferenceKeys.SubstitutePhotoFolder);
	}

	@Override
	protected boolean isResizable() {
		return true;
	}
}
