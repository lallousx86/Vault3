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

import java.text.MessageFormat;

import org.eclipse.jface.dialogs.IDialogConstants;
import org.eclipse.jface.dialogs.MessageDialog;
import org.eclipse.swt.SWT;
import org.eclipse.swt.events.HelpEvent;
import org.eclipse.swt.events.HelpListener;
import org.eclipse.swt.events.ModifyEvent;
import org.eclipse.swt.events.ModifyListener;
import org.eclipse.swt.events.SelectionEvent;
import org.eclipse.swt.events.SelectionListener;
import org.eclipse.swt.graphics.Color;
import org.eclipse.swt.graphics.Cursor;
import org.eclipse.swt.graphics.Image;
import org.eclipse.swt.layout.GridData;
import org.eclipse.swt.layout.GridLayout;
import org.eclipse.swt.widgets.Button;
import org.eclipse.swt.widgets.Combo;
import org.eclipse.swt.widgets.Composite;
import org.eclipse.swt.widgets.Control;
import org.eclipse.swt.widgets.Display;
import org.eclipse.swt.widgets.Label;
import org.eclipse.swt.widgets.List;
import org.eclipse.swt.widgets.Shell;
import org.eclipse.swt.widgets.Text;

/**
 * @author Eric Bergman-Terrell
 * 
 */
public class SendEmailDialog extends VaultDialog {
	@Override
	protected void populateFields() {
		bodyText.setText(body);
		fromText.setText(Globals.getPreferenceStore().getString(PreferenceKeys.EmailFromAddress));
		toText.setText(Globals.getPreferenceStore().getString(PreferenceKeys.EmailToAddress));

		for (String photoPath : photoPaths) {
			if (photoPath != null) {
				attachmentsList.add(photoPath);
			}
		}
	}

	private Combo scaleResolutionCombo;

	private Label statusLabel;

	private Color nonErrorBackground, errorBackground;

	private Button sendButton;

	private Text fromText, subjectText, toText, bodyText;

	private String selectedText;

	private String body;

	private java.util.List<String> photoPaths;

	private List attachmentsList;

	protected SendEmailDialog(Shell parentShell, String body, String selectedText, java.util.List<String> photoPaths) {
		super(parentShell);

		String newLine = PortabilityUtils.getNewLine();
		String vault3Promotion = MessageFormat.format("{1}{1}This email was sent to you by {0}. Download your copy from http://www.ericbt.com/Vault3{1}",
													  StringLiterals.ProgramName, newLine);

		this.body = MessageFormat.format("{0}{1}", body, vault3Promotion);

		if (selectedText != null && selectedText.length() > 0) {
			this.selectedText = MessageFormat.format("{0}{1}", selectedText, vault3Promotion);
		}

		this.photoPaths = photoPaths;
	}

	@Override
	protected boolean isResizable() {
		return true;
	}

	@Override
	protected void okPressed() {
		String toAddresses = toText.getText().replace(';', ',');
		String fromAddress = fromText.getText();
		String subject = subjectText.getText();

		Globals.getPreferenceStore().setValue(PreferenceKeys.EmailToAddress, toAddresses);
		Globals.getPreferenceStore().setValue(PreferenceKeys.EmailScaleImagesSelection,	scaleResolutionCombo.getSelectionIndex());

		final int[] resolutions = new int[] 
		{ -1, /* Full-Sized */
		  64, /* Thumbnail */
		 320, /* Tiny */
		 640, /* Small */
		 800, /* Medium */
		1024 /* Large */
		};

		int maxResolution = resolutions[scaleResolutionCombo.getSelectionIndex()];

		boolean successful = false;

		try {
			getShell().setCursor(new Cursor(getShell().getDisplay(), SWT.CURSOR_WAIT));

			Email.send(Globals.getPreferenceStore().getString(PreferenceKeys.EmailServerAddress), 
					   Globals.getPreferenceStore().getBoolean(PreferenceKeys.EmailAuthentication), 
					   Globals.getPreferenceStore().getString(PreferenceKeys.EmailUserName), 
					   Globals.getPreferenceStore().getString(PreferenceKeys.EmailPassword),
					toAddresses, fromAddress, subject, bodyText.getText(), photoPaths, maxResolution);
			successful = true;
		} catch (Throwable ex) {
			String message = MessageFormat.format("Cannot send email: {0}.", ex.getMessage());

			Image icon = Globals.getImageRegistry().get(Globals.IMAGE_REGISTRY_VAULT_ICON);

			MessageDialog messageDialog = new MessageDialog(Globals.getMainApplicationWindow().getShell(),
															StringLiterals.ProgramName, icon, message,
															MessageDialog.ERROR, new String[] { "&OK" }, 0);
			messageDialog.open();
		} finally {
			getShell().setCursor(null);
		}

		if (successful) {
			super.okPressed();
		}
	}

	@Override
	protected void createButtonsForButtonBar(Composite parent) {
		sendButton = createButton(parent, IDialogConstants.OK_ID, "&Send", true);
		createButton(parent, IDialogConstants.CANCEL_ID, "&Cancel", false);

		sendButton.setEnabled(false);
	}

	@Override
	protected Control createContents(Composite parent) {
		Control result = super.createContents(parent);

		statusLabel = new Label(parent, SWT.BORDER);

		GridData gridData = new GridData(GridData.FILL_HORIZONTAL);
		statusLabel.setLayoutData(gridData);

		nonErrorBackground = statusLabel.getBackground();
		errorBackground = Display.getCurrent().getSystemColor(SWT.COLOR_RED);

		enableDisableSendButton();

		return result;
	}

	private void enableDisableSendButton() {
		if (sendButton != null) {
			boolean enabled = true;

			String errorMessage = "";

			if (fromText == null || fromText.getText().trim().length() == 0) {
				errorMessage = "Please specify From email address.";
				enabled = false;
			} else if (toText == null || toText.getText().trim().length() == 0) {
				errorMessage = "Please specify To email address.";
				enabled = false;
			}

			sendButton.setEnabled(enabled);

			if (!enabled) {
				statusLabel.setText(errorMessage);
				statusLabel.setBackground(errorBackground);
			} else {
				statusLabel.setText("");
				statusLabel.setBackground(nonErrorBackground);
			}
		}
	}

	@Override
	protected void configureShell(Shell newShell) {
		super.configureShell(newShell);

		newShell.setText("Send Email");

	}

	@Override
	protected Control createDialogArea(Composite parent) {
		Composite composite = (Composite) super.createDialogArea(parent);
		composite.setLayout(new GridLayout(3, false));

		Label fromLabel = new Label(composite, SWT.NONE);
		fromLabel.setText("&From:");

		fromText = new Text(composite, SWT.BORDER);
		GridData gridData = new GridData();
		gridData.horizontalAlignment = SWT.FILL;
		gridData.grabExcessHorizontalSpace = true;
		fromText.setLayoutData(gridData);

		fromText.addModifyListener(new ModifyListener() {
			@Override
			public void modifyText(ModifyEvent e) {
				enableDisableSendButton();
			}
		});

		fromText.addFocusListener(new TextFocusListener());

		Image image = Globals.getImageRegistry().get(Globals.IMAGE_REGISTRY_LIGHTBULB);
		Label imageLabel = new Label(composite, SWT.NONE);
		imageLabel.setImage(image);
		imageLabel.setToolTipText("Enter your email address");

		Label toLabel = new Label(composite, SWT.NONE);
		toLabel.setText("&To:");

		toText = new Text(composite, SWT.BORDER);
		gridData = new GridData();
		gridData.horizontalAlignment = SWT.FILL;
		gridData.grabExcessHorizontalSpace = true;
		toText.setLayoutData(gridData);
		toText.addFocusListener(new TextFocusListener());

		image = Globals.getImageRegistry().get(Globals.IMAGE_REGISTRY_LIGHTBULB);
		imageLabel = new Label(composite, SWT.NONE);
		imageLabel.setImage(image);
		imageLabel.setToolTipText("Enter recipient's email address. If there are multiple recipients, separate the email addresses with commas or semicolons");

		toText.addModifyListener(new ModifyListener() {
			@Override
			public void modifyText(ModifyEvent e) {
				enableDisableSendButton();
			}
		});

		Label subjectLabel = new Label(composite, SWT.NONE);
		subjectLabel.setText("S&ubject:");

		subjectText = new Text(composite, SWT.BORDER);
		gridData = new GridData();
		gridData.horizontalSpan = 2;
		gridData.horizontalAlignment = SWT.FILL;
		gridData.grabExcessHorizontalSpace = true;
		subjectText.setLayoutData(gridData);
		subjectText.addFocusListener(new TextFocusListener());

		Label separatorLabel = new Label(composite, SWT.NONE);
		gridData = new GridData();
		gridData.horizontalSpan = 3;
		gridData.horizontalAlignment = SWT.FILL;
		gridData.grabExcessHorizontalSpace = true;
		separatorLabel.setLayoutData(gridData);

		Label bodyLabel = new Label(composite, SWT.NONE);
		bodyLabel.setText("&Body:");
		gridData = new GridData();
		gridData.horizontalSpan = 3;
		bodyLabel.setLayoutData(gridData);

		bodyText = new Text(composite, SWT.MULTI | SWT.V_SCROLL | SWT.WRAP
				| SWT.BORDER);
		bodyText.setBackground(subjectText.getBackground());
		bodyText.setForeground(subjectText.getForeground());

		if (selectedText != null && selectedText.trim().length() > 0) {
			final Button selectedTextOnlyButton = new Button(composite,	SWT.CHECK);
			selectedTextOnlyButton.setText("Selected Text &Only");

			selectedTextOnlyButton.addSelectionListener(new SelectionListener() {
				@Override
				public void widgetDefaultSelected(SelectionEvent e) {
				}

				@Override
				public void widgetSelected(SelectionEvent e) {
					bodyText.setText(selectedTextOnlyButton.getSelection() ? selectedText : body);
				}
			});
		}

		separatorLabel = new Label(composite, SWT.NONE);
		gridData = new GridData();
		gridData.horizontalSpan = 3;
		gridData.horizontalAlignment = SWT.FILL;
		gridData.grabExcessHorizontalSpace = true;
		separatorLabel.setLayoutData(gridData);

		gridData = new GridData();
		gridData.horizontalSpan = 3;
		gridData.horizontalAlignment = SWT.FILL;
		gridData.verticalAlignment = SWT.FILL;
		gridData.grabExcessHorizontalSpace = true;
		gridData.grabExcessVerticalSpace = true;
		bodyText.setLayoutData(gridData);

		Label attachmentsLabel = new Label(composite, SWT.NONE);
		attachmentsLabel.setText("&Attachments:");
		gridData = new GridData();
		gridData.horizontalSpan = 3;
		attachmentsLabel.setLayoutData(gridData);

		attachmentsList = new List(composite, SWT.SINGLE | SWT.V_SCROLL	| SWT.BORDER);

		gridData = new GridData();
		gridData.horizontalSpan = 3;
		gridData.horizontalAlignment = SWT.FILL;
		gridData.grabExcessHorizontalSpace = true;
		gridData.heightHint = attachmentsList.getItemHeight() * 5;
		attachmentsList.setLayoutData(gridData);

		Label scaleImagesLabel = new Label(composite, SWT.NONE);
		scaleImagesLabel.setText("Se&nd Attachments as:");

		scaleResolutionCombo = new Combo(composite, SWT.SINGLE | SWT.DROP_DOWN | SWT.READ_ONLY);
		scaleResolutionCombo.add("Full-Sized Images");
		scaleResolutionCombo.add("Thumbnail Images");
		scaleResolutionCombo.add("Tiny Images");
		scaleResolutionCombo.add("Small Images");
		scaleResolutionCombo.add("Medium Images");
		scaleResolutionCombo.add("Large Images");

		scaleResolutionCombo.setVisibleItemCount(scaleResolutionCombo.getItemCount());

		scaleResolutionCombo.select(Globals.getPreferenceStore().getInt(PreferenceKeys.EmailScaleImagesSelection));

		scaleResolutionCombo.setEnabled(photoPaths.size() > 0);

		composite.pack();

		parent.addHelpListener(new HelpListener() {
			@Override
			public void helpRequested(HelpEvent e) {
				HelpUtils.ProcessHelpRequest("Dialogs_SendEmailDialog");
			}
		});

		return composite;
	}
}
