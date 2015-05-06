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

import java.io.BufferedReader;
import java.io.IOException;
import java.io.InputStreamReader;
import java.net.URL;
import java.text.MessageFormat;
import java.util.Date;

import org.eclipse.jface.dialogs.IDialogConstants;
import org.eclipse.jface.dialogs.MessageDialog;
import org.eclipse.jface.preference.PreferenceStore;
import org.eclipse.swt.SWT;
import org.eclipse.swt.events.HelpEvent;
import org.eclipse.swt.events.HelpListener;
import org.eclipse.swt.events.SelectionEvent;
import org.eclipse.swt.events.SelectionListener;
import org.eclipse.swt.graphics.Image;
import org.eclipse.swt.layout.GridLayout;
import org.eclipse.swt.program.Program;
import org.eclipse.swt.widgets.Button;
import org.eclipse.swt.widgets.Composite;
import org.eclipse.swt.widgets.Control;
import org.eclipse.swt.widgets.Label;
import org.eclipse.swt.widgets.Shell;

public class SoftwareUpdatesDialog extends VaultDialog {
	private boolean updatesAreAvailable = false;
	
	private boolean cannotRetrieveLatestVersion = false;
	
	@Override
	protected boolean isResizable() {
		return false;
	}

	@Override
	protected void buttonPressed(int buttonId) {
		setReturnCode(buttonId);
		close();
	}

	@Override
	protected void createButtonsForButtonBar(Composite parent) {
		Button closeButton = createButton(parent, IDialogConstants.CLOSE_ID, IDialogConstants.CLOSE_LABEL, true);
		closeButton.forceFocus();
	}

	@Override
	protected Control createDialogArea(Composite parent) {
		Composite composite = (Composite) super.createDialogArea(parent);
		composite.setLayout(new GridLayout(1, true));
		
		Label updatesLabel = new Label(composite, SWT.NONE);
		
		if (!cannotRetrieveLatestVersion) {
			String updatesAvailableText = updatesAreAvailable ? MessageFormat.format("An updated version of {0} is available.", StringLiterals.ProgramName) : MessageFormat.format("You are running the latest version of {0}. Please check again in the future.", StringLiterals.ProgramName);
			updatesLabel.setText(updatesAvailableText);
		}
		else
		{
			updatesLabel.setText("Cannot check for updates. Please try again later.");
		}

		Label spacerLabel = new Label(composite, SWT.NONE);
		spacerLabel.setText("");

		// We want to unconditionally include the label and button, and hide them if necessary, leaving a gap in the dialog,
		// otherwise the dialog may be sized too small and the Donate button will be hidden.
		
		Label downloadLabel = new Label(composite, SWT.NONE);
		downloadLabel.setText("Click the Download Updates button to visit the download web page.");

		Button downloadUpdatesButton = new Button(composite, SWT.PUSH);
		downloadUpdatesButton.setText("Download &Updates");

		if (updatesAreAvailable) {
			downloadUpdatesButton.addSelectionListener(new SelectionListener() {
				@Override
				public void widgetDefaultSelected(SelectionEvent e) {
				}

				@Override
				public void widgetSelected(SelectionEvent e) {
					Program.launch("http://www.ericbt.com/Vault3/Download");
				}
			});
		}
		else {
			downloadLabel.setVisible(false);
			downloadUpdatesButton.setVisible(false);
		}

		parent.addHelpListener(new HelpListener() {
			@Override
			public void helpRequested(HelpEvent e) {
				HelpUtils.ProcessHelpRequest("Dialogs_CheckForUpdatesDialog");
			}
		});

		new Label(composite, SWT.NONE);
		
		new Label(composite, SWT.NONE).setText(MessageFormat.format("Click the Donate button to support continued {0} development.", StringLiterals.ProgramName));
		new Label(composite, SWT.NONE);
		
		Button donateButton = new Button(composite, SWT.PUSH);
		donateButton.setText("&Donate");

		donateButton.addSelectionListener(new SelectionListener() {
			@Override
			public void widgetDefaultSelected(SelectionEvent e) {
			}

			@Override
			public void widgetSelected(SelectionEvent e) {
				Program.launch(StringLiterals.SupportURL);
			}
		});

		new Label(composite, SWT.NONE).setText("");

		Button vault3ForAndroidButton = new Button(composite, SWT.PUSH);
		vault3ForAndroidButton.setText("&Vault 3 for Android");

		vault3ForAndroidButton.addSelectionListener(new SelectionListener() {
			@Override
			public void widgetDefaultSelected(SelectionEvent e) {
			}

			@Override
			public void widgetSelected(SelectionEvent e) {
				Program.launch(Globals.getPreferenceStore().getString(PreferenceKeys.Vault3ForAndroidURL));
			}
		});

		composite.pack();
		
		return composite;
	}

	public SoftwareUpdatesDialog(Shell parentShell, boolean updatesAreAvailable) {
		super(parentShell);

		this.updatesAreAvailable = updatesAreAvailable;
	}
	
	public SoftwareUpdatesDialog(Shell parentShell) {
		this(parentShell, false);
	}
	
	@Override
	protected void configureShell(Shell newShell) {
		super.configureShell(newShell);
		
		newShell.setText("Check for Updates");
		
		if (!updatesAreAvailable) {
			try {
				float latestVersion = getLatestVersion();
				
				updatesAreAvailable = latestVersion > Version.getVersionNumber();
			}
			catch (Throwable ex) {
				ex.printStackTrace();

				cannotRetrieveLatestVersion = true;

				Image icon = Globals.getImageRegistry().get(Globals.IMAGE_REGISTRY_VAULT_ICON);
				MessageDialog messageDialog = new MessageDialog(getShell(), StringLiterals.ProgramName, icon, "Error checking for updates.", MessageDialog.ERROR, new String[] { "&OK" }, 0);
				messageDialog.open();
			}
		}
	}
	
	private static float getLatestVersion() throws IOException {
		float latestVersion = 0.00f;
		
		URL versionURL = new URL("http://www.EricBT.com/versions/vault3.txt");

		BufferedReader bufferedReader = null;
		StringBuilder text = new StringBuilder();
		
		try {
			bufferedReader = new BufferedReader(new InputStreamReader(versionURL.openStream()));
			String inputLine;
	
			while ((inputLine = bufferedReader.readLine()) != null) {
				// Process each line.
				text.append(inputLine);
				text.append(PortabilityUtils.getNewLine());
			}
			
			latestVersion = Float.valueOf(text.toString().trim());
			
			Globals.getPreferenceStore().setValue(PreferenceKeys.LastUpdateCheckDate, new Date().getTime());
		}
		finally {
			if (bufferedReader != null) {
				bufferedReader.close();
			}
		}
		
		return latestVersion;
	}
	
	public static void displayUpdatesDialogIfUpdatesAreAvailable(Shell parentShell) {
		try {
			PreferenceStore preferenceStore = Globals.getPreferenceStore();
			
			if (preferenceStore.getBoolean(PreferenceKeys.CheckForUpdatesAutomatically)) {
				long lastCheckInstant = preferenceStore.getLong(PreferenceKeys.LastUpdateCheckDate);
				long now = new Date().getTime();
				
				final long sevenDaysInMilliseconds = 1000 * 60 * 60 * 24 * 7;
	
				if ((now - lastCheckInstant) >= sevenDaysInMilliseconds) {
					if (getLatestVersion() > Version.getVersionNumber()) {
						SoftwareUpdatesDialog softwareUpdatesDialog = new SoftwareUpdatesDialog(parentShell, true);
						
						softwareUpdatesDialog.open();
					}
				}
			}
		}
		catch (Throwable ex) {
			ex.printStackTrace();
		}
	}
}
