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

import org.eclipse.jface.action.Action;
import org.eclipse.jface.dialogs.IDialogConstants;
import org.eclipse.jface.dialogs.MessageDialog;
import org.eclipse.jface.resource.ImageDescriptor;
import org.eclipse.jface.viewers.ISelectionChangedListener;
import org.eclipse.jface.viewers.SelectionChangedEvent;
import org.eclipse.swt.SWT;
import org.eclipse.swt.graphics.Image;
import org.eclipse.swt.graphics.Point;
import org.eclipse.swt.widgets.Display;

/**
 * @author Eric Bergman-Terrell
 *
 */
public class FileActions {
	public static class NewAction extends Action {
		@Override
		public String getDescription() {
			return "Create a new document";
		}

		public NewAction() {
			super("New", ImageDescriptor.createFromImage(new Image(Display.getCurrent(), MainApplicationWindow.class.getResourceAsStream("artwork/file_new.png"))));
			setAccelerator(SWT.MOD1 | 'N');
			setId(HelpUtils.helpIDFromClass(this));
		}
		
		public void run() {
			boolean cancelled = Globals.getMainApplicationWindow().saveCurrentDocument();
			
			if (!cancelled) {
				Globals.getVaultTreeViewer().fileNew();
				Globals.getMainApplicationWindow().getSearchUI().reset();
				Globals.getMainApplicationWindow().notifyDocumentLoadUnloadListeners();
			}
		}
	}
	
	public static class OpenAction extends Action {
		@Override
		public String getDescription() {
			return "Open an existing document";
		}

		public OpenAction() {
			super("&Open", ImageDescriptor.createFromImage(new Image(Display.getCurrent(), MainApplicationWindow.class.getResourceAsStream("artwork/file_open.png"))));
			setAccelerator(SWT.MOD1 | 'O');
			setEnabled(true);
			setId(HelpUtils.helpIDFromClass(this));
		}
		
		public void run() {
			String filePath = null;
			StringWrapper filePathStringWrapper = new StringWrapper(filePath);
			
			try {
				boolean cancelled = Globals.getMainApplicationWindow().saveCurrentDocument();
				
				if (!cancelled) {
					filePath = VaultDocumentIO.fileOpen(Globals.getMainApplicationWindow().getShell(), filePathStringWrapper);
					Globals.getMainApplicationWindow().getSearchUI().reset();
					Globals.getVaultTreeViewer().selectFirstItem();
					Globals.getMainApplicationWindow().notifyDocumentLoadUnloadListeners();
				}
			}
			catch (Throwable ex) {
				boolean processedException = DatabaseVersionTooHigh.displayMessaging(ex, filePathStringWrapper.getValue());

				if (!processedException) {
					String message = MessageFormat.format("Cannot open file {2}.{0}{0}{1}", PortabilityUtils.getNewLine(),  ex.getMessage(), filePathStringWrapper.getValue());
					MessageDialog messageDialog = new MessageDialog(Globals.getMainApplicationWindow().getShell(), StringLiterals.ProgramName, Globals.getImageRegistry().get(Globals.IMAGE_REGISTRY_VAULT_ICON), message, MessageDialog.ERROR, new String[] { "&OK" }, 0);
					messageDialog.open();
				}

				ex.printStackTrace();
			}
		}
	}
	
	public static class SaveAction extends Action {
		@Override
		public String getDescription() {
			return "Save the active document";
		}

		public SaveAction() {
			super("&Save", ImageDescriptor.createFromImage(new Image(Display.getCurrent(), MainApplicationWindow.class.getResourceAsStream("artwork/file_save.png"))));
			setAccelerator(SWT.MOD1 | 'S');
			setId(HelpUtils.helpIDFromClass(this));
		}
		
		public void run() {
			try {
				VaultDocumentIO.fileSave(Globals.getMainApplicationWindow().getShell());
			}
			catch (Throwable ex) {
				String message = MessageFormat.format("Cannot save file.{0}{0}{1}", PortabilityUtils.getNewLine(),  ex.getMessage());
				MessageDialog messageDialog = new MessageDialog(Globals.getMainApplicationWindow().getShell(), StringLiterals.ProgramName, Globals.getImageRegistry().get(Globals.IMAGE_REGISTRY_VAULT_ICON), message, MessageDialog.ERROR, new String[] { "&OK" }, 0);
				messageDialog.open();

				ex.printStackTrace();
			}
		}
	}
	
	public static class SaveAsAction extends Action {
		@Override
		public String getDescription() {
			return "Save the active document with a new name";
		}

		public SaveAsAction() {
			super("Save &As...");
			setId(HelpUtils.helpIDFromClass(this));
		}
		
		public void run() {
			try {
				VaultDocumentIO.fileSaveAs(Globals.getMainApplicationWindow().getShell());
			}
			catch (Throwable ex) {
				String message = MessageFormat.format("Cannot save file.{0}{0}{1}", PortabilityUtils.getNewLine(),  ex.getMessage());
				MessageDialog messageDialog = new MessageDialog(Globals.getMainApplicationWindow().getShell(), StringLiterals.ProgramName, Globals.getImageRegistry().get(Globals.IMAGE_REGISTRY_VAULT_ICON), message, MessageDialog.ERROR, new String[] { "&OK" }, 0);
				messageDialog.open();

				ex.printStackTrace();
			}
		}
	}
	
	public static class PasswordAction extends Action {
		@Override
		public String getDescription() {
			return "Specify whether or not a password is required to access this document";
		}

		public PasswordAction() {
			super("Pass&word...", ImageDescriptor.createFromImage(new Image(Display.getCurrent(), MainApplicationWindow.class.getResourceAsStream("artwork/key.png"))));
			setId(HelpUtils.helpIDFromClass(this));
		}
		
		public void run() {
			PasswordDialog passwordDialog = new PasswordDialog(Globals.getMainApplicationWindow().getShell(), Globals.getVaultDocument().getPassword());
			
			if (passwordDialog.open() == IDialogConstants.OK_ID) {
				Globals.getVaultDocument().setPassword(passwordDialog.getPassword());
			}
		}
	}
	
	public static class PrintAction extends Action implements ISelectionChangedListener {
		@Override
		public String getDescription() {
			return "Print the active document";
		}

		public PrintAction() {
			super("&Print", ImageDescriptor.createFromImage(new Image(Display.getCurrent(), MainApplicationWindow.class.getResourceAsStream("artwork/file_print.png"))));
			setAccelerator(SWT.MOD1 | 'P');
			setEnabled(false);
			setId(HelpUtils.helpIDFromClass(this));
		}
		
		public void setEnabled() {
			setEnabled(Printing.canPrint());
		}
		
		public void run() {
			if (Printing.canPrint()) {
				Globals.getVaultTextViewer().saveChanges();
				Printing.print(Globals.getMainApplicationWindow().getShell());
			}
		}

		@Override
		public void selectionChanged(SelectionChangedEvent event) {
			setEnabled();
		}
	}
	
	public static class ImportFromXMLFileAction extends Action implements ISelectionChangedListener {
		@Override
		public String getDescription() {
			return MessageFormat.format("Insert an XML file ({0}) exported from Vault or The Photo Program into the current {1} document", StringLiterals.XMLFileTypeWildcarded, StringLiterals.ProgramName);
		}

		public ImportFromXMLFileAction() {
			super("&Import Vault or The Photo Program XML File...");
			setEnabled(true);
			setId(HelpUtils.helpIDFromClass(this));
		}
		
		public void setEnabled() {
			setEnabled(VaultDocumentImports.canFileImport());
		}
		
		public void run() {
			try {
				VaultDocumentImports.legacyXMLFileImport(Globals.getMainApplicationWindow().getShell());
			}
			catch (Throwable ex) {
				String message = MessageFormat.format("Cannot Import XML File.{0}{0}{1}", PortabilityUtils.getNewLine(), ex.getMessage());
				MessageDialog messageDialog = new MessageDialog(Globals.getMainApplicationWindow().getShell(), StringLiterals.ProgramName, Globals.getImageRegistry().get(Globals.IMAGE_REGISTRY_VAULT_ICON), message, MessageDialog.ERROR, new String[] { "&OK" }, 0);
				messageDialog.open();

				ex.printStackTrace();
			}
		}

		@Override
		public void selectionChanged(SelectionChangedEvent event) {
			setEnabled();
		}
	}

	public static class ImportFromVault3FileAction extends Action implements ISelectionChangedListener {
		@Override
		public String getDescription() {
			return MessageFormat.format("Insert an XML file previously exported from {0} into the current {0} document", StringLiterals.ProgramName, StringLiterals.ProgramFileTypeWildcarded, StringLiterals.XMLFileTypeWildcarded);
		}

		public ImportFromVault3FileAction() {
			super(MessageFormat.format("Import {0} XML File...", StringLiterals.ProgramName));
			setEnabled(true);
			setId(HelpUtils.helpIDFromClass(this));
		}
		
		public void setEnabled() {
			setEnabled(VaultDocumentImports.canFileImport());
		}
		
		public void run() {
			try {
				VaultDocumentImports.vault3FileImport(Globals.getMainApplicationWindow().getShell());
			}
			catch (Throwable ex) {
				String message = MessageFormat.format("Cannot Import {2} File.{0}{0}{1}", PortabilityUtils.getNewLine(), ex.getMessage(), StringLiterals.ProgramName);
				MessageDialog messageDialog = new MessageDialog(Globals.getMainApplicationWindow().getShell(), StringLiterals.ProgramName, Globals.getImageRegistry().get(Globals.IMAGE_REGISTRY_VAULT_ICON), message, MessageDialog.ERROR, new String[] { "&OK" }, 0);
				messageDialog.open();

				ex.printStackTrace();
			}
		}

		@Override
		public void selectionChanged(SelectionChangedEvent event) {
			setEnabled();
		}
	}

	public static class ImportFromFileSystemAction extends Action implements ISelectionChangedListener {
		@Override
		public String getDescription() {
			return "Import text files, photos, and videos from a filesystem folder";
		}

		public ImportFromFileSystemAction() {
			super("&Import Text Files, Photos, and Videos from Disk Folder...");
			setId(HelpUtils.helpIDFromClass(this));
			setEnabled(true);
		}
		
		private void setEnabled() {
			setEnabled(VaultDocumentImports.canFolderImport());
		}
		
		public void run() {
			try {
				VaultDocumentImports.folderImport(Globals.getMainApplicationWindow().getShell());
			}
			catch (Throwable ex) {
				String message = MessageFormat.format("Cannot import from folder.{0}{0}{1}", PortabilityUtils.getNewLine(), ex.getMessage());
				MessageDialog messageDialog = new MessageDialog(Globals.getMainApplicationWindow().getShell(), StringLiterals.ProgramName, Globals.getImageRegistry().get(Globals.IMAGE_REGISTRY_VAULT_ICON), message, MessageDialog.ERROR, new String[] { "&OK" }, 0);
				messageDialog.open();

				ex.printStackTrace();
			}
		}

		@Override
		public void selectionChanged(SelectionChangedEvent event) {
			setEnabled();
		}
	}

	public static class XMLExportAction extends Action implements ISelectionChangedListener {
		@Override
		public String getDescription() {
			return "Export the selected items to an XML file";
		}

		public XMLExportAction() {
			super("&Export Selected Items to XML File...");
			setEnabled(false);
			setId(HelpUtils.helpIDFromClass(this));
		}
		
		public void setEnabled() {
			setEnabled(VaultDocumentExports.canXmlFileExport());
		}
		
		public void run() {
			try {
				VaultDocumentExports.xmlFileExport(Globals.getMainApplicationWindow().getShell());
			}
			catch (Throwable ex) {
				ex.printStackTrace();
				
				String message = MessageFormat.format("Cannot export.{0}{0}{1}", PortabilityUtils.getNewLine(),  ex.getMessage());
				MessageDialog messageDialog = new MessageDialog(Globals.getMainApplicationWindow().getShell(), StringLiterals.ProgramName, Globals.getImageRegistry().get(Globals.IMAGE_REGISTRY_VAULT_ICON), message, MessageDialog.ERROR, new String[] { "&OK" }, 0);
				messageDialog.open();
			}
		}

		@Override
		public void selectionChanged(SelectionChangedEvent event) {
			setEnabled();
		}
	}

	public static class PDFExportAction extends Action implements ISelectionChangedListener {
		@Override
		public String getDescription() {
			return "Export the selected items to an XML file";
		}

		public PDFExportAction() {
			super("Export Selected Items to &PDF File...");
			setEnabled(false);
			setId(HelpUtils.helpIDFromClass(this));
		}
		
		public void setEnabled() {
			setEnabled(VaultDocumentExports.canPDFFileExport());
		}
		
		public void run() {
			try {
				VaultDocumentExports.pdfFileExport(Globals.getMainApplicationWindow().getShell());
			}
			catch (Throwable ex) {
				ex.printStackTrace();
				
				String message = MessageFormat.format("Cannot export.{0}{0}{1}", PortabilityUtils.getNewLine(),  ex.getMessage());
				MessageDialog messageDialog = new MessageDialog(Globals.getMainApplicationWindow().getShell(), StringLiterals.ProgramName, Globals.getImageRegistry().get(Globals.IMAGE_REGISTRY_VAULT_ICON), message, MessageDialog.ERROR, new String[] { "&OK" }, 0);
				messageDialog.open();
			}
		}

		@Override
		public void selectionChanged(SelectionChangedEvent event) {
			setEnabled();
		}
	}

	public static class ExportPhotosToDeviceAction extends Action implements ISelectionChangedListener {
		@Override
		public String getDescription() {
			return "Export selected photo(s) to device";
		}

		public ExportPhotosToDeviceAction() {
			super("Export Photos to &Device...");
			setEnabled(false);
			setId(HelpUtils.helpIDFromClass(this));
		}
		
		public void setEnabled() {
			setEnabled(VaultDocumentExports.canExportPhotosToDevice());
		}
		
		public void run() {
			try {
				// Need to save changes to current item, in case this affects the exclusions.
				Globals.getVaultTextViewer().saveChanges();

				ExportPhotosToDeviceDialog exportPhotosToDeviceDialog = new ExportPhotosToDeviceDialog(Globals.getMainApplicationWindow().getShell());
				
				if (exportPhotosToDeviceDialog.open() != IDialogConstants.CANCEL_ID) {
					Point deviceDimensions = new Point(Globals.getPreferenceStore().getInt(PreferenceKeys.ExportPhotosWidth),
							                           Globals.getPreferenceStore().getInt(PreferenceKeys.ExportPhotosHeight));

					boolean shuffle = Globals.getPreferenceStore().getBoolean(PreferenceKeys.ExportPhotosShuffle);
					String destinationFolder = Globals.getPreferenceStore().getString(PreferenceKeys.ExportPhotosDestFolder);
					int maxPhotosPerFolder = Globals.getPreferenceStore().getInt(PreferenceKeys.ExportPhotosPhotosPerFolder);
					int maxPhotos = Globals.getPreferenceStore().getInt(PreferenceKeys.ExportPhotosTotalPhotos);
					boolean deleteFolderContents = Globals.getPreferenceStore().getBoolean(PreferenceKeys.ExportPhotosDeleteFolderContents);
					
					VaultDocumentExports.exportPhotosToDevice(Globals.getMainApplicationWindow().getShell(), deviceDimensions, destinationFolder, maxPhotos, maxPhotosPerFolder, shuffle, deleteFolderContents);
				}
			}
			catch (Throwable ex) {
				ex.printStackTrace();
				
				String message = MessageFormat.format("Cannot export photo file(s) to device.{0}{0}{1}", PortabilityUtils.getNewLine(),  ex.getMessage());
				MessageDialog messageDialog = new MessageDialog(Globals.getMainApplicationWindow().getShell(), StringLiterals.ProgramName, Globals.getImageRegistry().get(Globals.IMAGE_REGISTRY_VAULT_ICON), message, MessageDialog.ERROR, new String[] { "&OK" }, 0);
				messageDialog.open();
			}
		}

		@Override
		public void selectionChanged(SelectionChangedEvent event) {
			setEnabled();
		}
	}
	
	public static class CopyPictureFileAction extends Action implements ISelectionChangedListener {
		@Override
		public String getDescription() {
			return "Copy the current photo file to a new file";
		}

		public CopyPictureFileAction() {
			super("&Copy Photo File...");
			setEnabled(false);
			setId(HelpUtils.helpIDFromClass(this));
		}
		
		public void setEnabled() {
			setEnabled(PhotoProcessing.canCopyPictureFile());
		}
		
		public void run() {
			try {
				PhotoProcessing.copyPictureFile(Globals.getMainApplicationWindow().getShell());
			}
			catch (Throwable ex) {
				ex.printStackTrace();
				
				String message = MessageFormat.format("Cannot copy photo file.{0}{0}{1}", PortabilityUtils.getNewLine(),  ex.getMessage());
				MessageDialog messageDialog = new MessageDialog(Globals.getMainApplicationWindow().getShell(), StringLiterals.ProgramName, Globals.getImageRegistry().get(Globals.IMAGE_REGISTRY_VAULT_ICON), message, MessageDialog.ERROR, new String[] { "&OK" }, 0);
				messageDialog.open();
			}
		}

		@Override
		public void selectionChanged(SelectionChangedEvent event) {
			setEnabled();
		}
	}
	
	public static class DeletePictureFileAction extends Action implements ISelectionChangedListener {
		@Override
		public String getDescription() {
			return "Delete the current photo file";
		}

		public DeletePictureFileAction() {
			super("&Delete Photo File...");
			setEnabled(false);
			setId(HelpUtils.helpIDFromClass(this));
		}
		
		public void setEnabled() {
			setEnabled(PhotoProcessing.canDeletePictureFile());
		}
		
		public void run() {
			try {
				PhotoProcessing.deletePictureFile(Globals.getMainApplicationWindow().getShell());
			}
			catch (Throwable ex) {
				ex.printStackTrace();
				
				String message = MessageFormat.format("Cannot delete photo file.{0}{0}{1}", PortabilityUtils.getNewLine(),  ex.getMessage());
				MessageDialog messageDialog = new MessageDialog(Globals.getMainApplicationWindow().getShell(), StringLiterals.ProgramName, Globals.getImageRegistry().get(Globals.IMAGE_REGISTRY_VAULT_ICON), message, MessageDialog.ERROR, new String[] { "&OK" }, 0);
				messageDialog.open();
			}
		}

		@Override
		public void selectionChanged(SelectionChangedEvent event) {
			setEnabled();
		}
	}
	
	public static class RenamePictureFileAction extends Action implements ISelectionChangedListener {
		@Override
		public String getDescription() {
			return "Rename the current photo file";
		}

		public RenamePictureFileAction() {
			super("&Rename Photo File...");
			setEnabled(false);
			setId(HelpUtils.helpIDFromClass(this));
		}
		
		public void setEnabled() {
			setEnabled(PhotoProcessing.canRenamePictureFile());
		}
		
		public void run() {
			try {
				PhotoProcessing.renamePictureFile(Globals.getMainApplicationWindow().getShell());
			}
			catch (Throwable ex) {
				ex.printStackTrace();
				
				String message = MessageFormat.format("Cannot rename photo file.{0}{0}{1}", PortabilityUtils.getNewLine(),  ex.getMessage());
				MessageDialog messageDialog = new MessageDialog(Globals.getMainApplicationWindow().getShell(), StringLiterals.ProgramName, Globals.getImageRegistry().get(Globals.IMAGE_REGISTRY_VAULT_ICON), message, MessageDialog.ERROR, new String[] { "&OK" }, 0);
				messageDialog.open();
			}
		}

		@Override
		public void selectionChanged(SelectionChangedEvent event) {
			setEnabled();
		}
	}
	
	public static class EditPictureFileAction extends Action implements ISelectionChangedListener {
		@Override
		public String getDescription() {
			return "Edit the current photo file";
		}

		public EditPictureFileAction() {
			super("Edit P&hoto File...");
			setEnabled(false);
			setId(HelpUtils.helpIDFromClass(this));
		}
		
		public void setEnabled() {
			setEnabled(PhotoProcessing.canEditPictureFile());
		}
		
		public void run() {
			try {
				PhotoProcessing.editPictureFile();
			}
			catch (Throwable ex) {
				ex.printStackTrace();
				
				String message = MessageFormat.format("Cannot edit picture file.{0}{0}{1}", PortabilityUtils.getNewLine(),  ex.getMessage());
				MessageDialog messageDialog = new MessageDialog(Globals.getMainApplicationWindow().getShell(), StringLiterals.ProgramName, Globals.getImageRegistry().get(Globals.IMAGE_REGISTRY_VAULT_ICON), message, MessageDialog.ERROR, new String[] { "&OK" }, 0);
				messageDialog.open();
			}
		}

		@Override
		public void selectionChanged(SelectionChangedEvent event) {
			setEnabled();
		}
	}
	
	public static class TextExportAction extends Action implements ISelectionChangedListener {
		@Override
		public String getDescription() {
			return "Export selected items to a text File";
		}

		public TextExportAction() {
			super("Export &Selected Items to Text File...");
			setEnabled(false);
			setId(HelpUtils.helpIDFromClass(this));
		}

		public void setEnabled() {
			setEnabled(VaultDocumentExports.canTextFileExport());
		}
		
		public void run() {
			try {
				VaultDocumentExports.textFileExport(Globals.getMainApplicationWindow().getShell());
			}
			catch (Throwable ex) {
				ex.printStackTrace();

				String message = MessageFormat.format("Cannot export.{0}{0}{1}", PortabilityUtils.getNewLine(),  ex.getMessage());
				MessageDialog messageDialog = new MessageDialog(Globals.getMainApplicationWindow().getShell(), StringLiterals.ProgramName, Globals.getImageRegistry().get(Globals.IMAGE_REGISTRY_VAULT_ICON), message, MessageDialog.ERROR, new String[] { "&OK" }, 0);
				messageDialog.open();
			}
		}

		@Override
		public void selectionChanged(SelectionChangedEvent event) {
			setEnabled();
		}
	}

	public static class EmailAction extends Action implements ISelectionChangedListener {
		@Override
		public String getDescription() {
			return "Send selected items via email"; 
		}
		
		public EmailAction() {
			super("Send E&mail...", ImageDescriptor.createFromImage(new Image(Display.getCurrent(), MainApplicationWindow.class.getResourceAsStream("artwork/email.png"))));
			setEnabled(false);
			setId(HelpUtils.helpIDFromClass(this));
		}

		public void setEnabled() {
			setEnabled(EmailUI.canEmail());
		}
		
		public void run() {
			try {
				Globals.getVaultTextViewer().saveChanges();
				EmailUI.email();
			}
			catch (Throwable ex) {
				ex.printStackTrace();

				String message = MessageFormat.format("Cannot email.{0}{0}{1}", PortabilityUtils.getNewLine(),  ex.getMessage());
				MessageDialog messageDialog = new MessageDialog(Globals.getMainApplicationWindow().getShell(), StringLiterals.ProgramName, Globals.getImageRegistry().get(Globals.IMAGE_REGISTRY_VAULT_ICON), message, MessageDialog.ERROR, new String[] { "&OK" }, 0);
				messageDialog.open();
			}
		}

		@Override
		public void selectionChanged(SelectionChangedEvent event) {
			setEnabled();
		}
	}
	
	public static class ExitAction extends Action {
		@Override
		public String getDescription() {
			return "Quit the application; prompts to save the document"; 
		}

		public ExitAction() {
			super("E&xit");
			setId(HelpUtils.helpIDFromClass(this));
		}
		
		public void run() {
			Globals.getMainApplicationWindow().getShell().close();
		}
	}
}
