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

import java.util.*;

import mainPackage.OutlineItem.AddDirection;

import org.eclipse.jface.action.MenuManager;
import org.eclipse.jface.action.Separator;
import org.eclipse.jface.dialogs.IDialogConstants;
import org.eclipse.jface.dialogs.MessageDialog;
import org.eclipse.jface.viewers.ISelection;
import org.eclipse.jface.viewers.IStructuredSelection;
import org.eclipse.jface.viewers.ITreeViewerListener;
import org.eclipse.jface.viewers.SelectionChangedEvent;
import org.eclipse.jface.viewers.StructuredSelection;
import org.eclipse.jface.viewers.TreeViewer;
import org.eclipse.swt.SWT;
import org.eclipse.swt.events.KeyEvent;
import org.eclipse.swt.events.KeyListener;
import org.eclipse.swt.events.MenuEvent;
import org.eclipse.swt.events.MenuListener;
import org.eclipse.swt.events.MouseEvent;
import org.eclipse.swt.events.MouseListener;
import org.eclipse.swt.events.TreeListener;
import org.eclipse.swt.graphics.Font;
import org.eclipse.swt.graphics.FontData;
import org.eclipse.swt.graphics.Image;
import org.eclipse.swt.graphics.Point;
import org.eclipse.swt.widgets.Composite;
import org.eclipse.swt.widgets.Control;
import org.eclipse.swt.widgets.Display;
import org.eclipse.swt.widgets.FontDialog;
import org.eclipse.swt.widgets.Shell;
import org.eclipse.swt.widgets.TreeItem;

/**
 * @author Eric Bergman-Terrell
 *
 */
public class VaultTreeViewer extends TreeViewer {
	private List<ITreeViewerListener> treeViewerListeners;

	private boolean usingNonDefaultFont = false, menusArmed = false;

	private Font font = null;
	
	private enum ExpandCollapseAction { Expand, Collapse }
	
	private void initialize(Composite parent, boolean readOnly)
	{
		treeViewerListeners = new ArrayList<>();

		setAutoExpandLevel(ALL_LEVELS);

		MainApplicationWindow mainApplicationWindow = Globals.getMainApplicationWindow();

		if (!readOnly) {
			final MenuManager menuManager = new MenuManager();
			//menuManager.add(mainApplicationWindow.getAddAction());
			menuManager.add(mainApplicationWindow.getAction(OutlineActions.AddAction.class));
			menuManager.add(mainApplicationWindow.getAction(OutlineActions.EditAction.class));
			menuManager.add(mainApplicationWindow.getAction(OutlineActions.ImportPicturesAction.class));
			menuManager.add(new Separator());
			menuManager.add(mainApplicationWindow.getAction(OutlineActions.MoveAction.class));
			menuManager.add(mainApplicationWindow.getAction(OutlineActions.MoveUpAction.class));
			menuManager.add(mainApplicationWindow.getAction(OutlineActions.MoveDownAction.class));
			menuManager.add(new Separator());
			menuManager.add(mainApplicationWindow.getAction(OutlineActions.IndentAction.class));
			menuManager.add(mainApplicationWindow.getAction(OutlineActions.UnindentAction.class));
			menuManager.add(new Separator());
			menuManager.add(mainApplicationWindow.getAction(OutlineActions.ExpandAction.class));
			menuManager.add(mainApplicationWindow.getAction(OutlineActions.ExpandAllAction.class));
			menuManager.add(new Separator());
			menuManager.add(mainApplicationWindow.getAction(OutlineActions.CollapseAction.class));
			menuManager.add(mainApplicationWindow.getAction(OutlineActions.CollapseAllAction.class));
			menuManager.add(new Separator());
			menuManager.add(mainApplicationWindow.getAction(OutlineActions.SortAction.class));
			menuManager.add(new Separator());
			menuManager.add(mainApplicationWindow.getAction(OutlineActions.SelectAllAction.class));
			menuManager.add(new Separator());
			menuManager.add(mainApplicationWindow.getAction(OutlineActions.CutAction.class));
			menuManager.add(mainApplicationWindow.getAction(OutlineActions.CopyAction.class));
			menuManager.add(mainApplicationWindow.getAction(OutlineActions.PasteAction.class));
			menuManager.add(new Separator());
			menuManager.add(mainApplicationWindow.getAction(OutlineActions.RemoveAction.class));
			menuManager.add(new Separator());
			menuManager.add(mainApplicationWindow.getAction(OutlineActions.SetFontAction.class));
	
			getControl().setMenu(menuManager.createContextMenu(parent));
			
			getControl().getMenu().addMenuListener(new MenuListener() {
				@Override
				public void menuHidden(MenuEvent e) {
					Globals.getMainApplicationWindow().setStatusLineMessage("");
				}
	
				@Override
				public void menuShown(MenuEvent e) {
					if (!menusArmed) {
						MenuUtils.armAllMenuItems(menuManager);
						menusArmed = true;
					}

					// Move the cursor over a bit to avoid selecting the first menu item on Ubuntu.
					Point cursorLocation = e.display.getCursorLocation();
					
					Display.getCurrent().setCursorLocation(cursorLocation.x + 1, cursorLocation.y + 1);
				}
			});
			
			getTree().addMouseListener(new MouseListener() {
				@Override
				public void mouseDoubleClick(MouseEvent e) {
					toggle();
				}
	
				@Override
				public void mouseDown(MouseEvent e) {
				}
	
				@Override
				public void mouseUp(MouseEvent e) {
				}
			});
		}
		
		getTree().addKeyListener(new KeyListener() {
			@Override
			public void keyPressed(KeyEvent e) {
			}

			@Override
			public void keyReleased(KeyEvent e) {
				// Don't want to do anything if the user is specifying a keyboard accelerator.
				if ((e.stateMask & (SWT.ALT | SWT.CTRL)) == 0) {
					if (e.keyCode == (int) '\r') {
						toggle();
					}
					else if (e.keyCode == SWT.ARROW_RIGHT) {
						arrowKeyExpandCollapse(ExpandCollapseAction.Expand);
					}
					else if (e.keyCode == SWT.ARROW_LEFT) {
						arrowKeyExpandCollapse(ExpandCollapseAction.Collapse);
					}
				}
			}
		});
	}
	
	public VaultTreeViewer(Composite parent, int style, boolean readOnly) {
		super(parent, style);
		
		initialize(parent, readOnly);
	}
	
	public VaultTreeViewer(Composite parent, int style) {
		super(parent, style);

		initialize(parent, false);
	}

	public void dispose() {
		if (font != null) {
			font.dispose();
		}
	}
	
	/**
	 * Toggle the expanded/collapsed state of the single selected item when the user presses Enter.
	 */
	private void toggle() {
		TreeItem[] selectedItems = getTree().getSelection();
		
		if (selectedItems.length == 1) {
			if (canExpand(selectedItems[0])) {
				expandSelectedItems();
			}
			else if (canCollapse(selectedItems[0])) {
				collapseSelectedItems();
			}
		}
	}

	// Expand when right arrow key pressed in Linux.
	private void arrowKeyExpandCollapse(ExpandCollapseAction action) {
		TreeItem[] selectedItems = getTree().getSelection();
		
		if (selectedItems.length == 1) {
			if (action == ExpandCollapseAction.Expand && canExpand(selectedItems[0])) {
				expandSelectedItems();
			}
			else if (action == ExpandCollapseAction.Collapse && canCollapse(selectedItems[0])) {
				collapseSelectedItems();
			}
		}
	}
	
	public void applyUserPreferences() {
		String fontString = Globals.getPreferenceStore().getString(PreferenceKeys.OutlineFontString);
		FontData fontData[] = FontUtils.stringToFontList(fontString);

		if (fontData != null) {
			font = new Font(getTree().getDisplay(), fontData);
			
			Font previousFont = getTree().getFont();
			
			getTree().setFont(font);
			
			if (usingNonDefaultFont) {
				previousFont.dispose();
			}
			
			usingNonDefaultFont = true;
		}
		
		int red   = Globals.getPreferenceStore().getInt(PreferenceKeys.OutlineFontRed);
		int green = Globals.getPreferenceStore().getInt(PreferenceKeys.OutlineFontGreen);
		int blue  = Globals.getPreferenceStore().getInt(PreferenceKeys.OutlineFontBlue);
		
		if (ColorUtils.rgbValueIsValid(red, green, blue)) {
			getTree().setForeground(Globals.getColorRegistry().get(red, green, blue));
		}
	}
	
	public boolean canSelectAll() {
		return getTree().getItemCount() > 0;
	}

	public void selectAll() {
		// To save time, just select top-level items.
		getControl().forceFocus();
		getTree().setSelection(getTree().getItems());
	}
	
	public void selectFirstItem() {
		getControl().forceFocus();
		
		if (Globals.getVaultDocument().getContent().getChildren().size() >= 1) {
			Display.getCurrent().asyncExec(new Runnable() {
		        public void run() {
					StructuredSelection structuredSelection = new StructuredSelection(Globals.getVaultDocument().getContent().getChildren().get(0));
					
					setSelection(structuredSelection);
					
					// Force selected item to be rendered with the proper font and color.
					SelectionChangedEvent event = new SelectionChangedEvent(VaultTreeViewer.this, structuredSelection);
					Globals.getVaultTextViewer().selectionChanged(event);
		        }
			});
		}
	}
	
	public void load(OutlineItem rootNode) {
		getTree().setRedraw(false);

		// Drop references to the old document data to avoid excessive garbage collection.
		setInput(null);
		
		setInput(rootNode);
		
		collapseAll();
		getTree().setRedraw(true);
	}
	
	public void removeRedundantItems(List<OutlineItem> originalList) {
		boolean itemRemoved;
		
		do {
			itemRemoved = false;

			for (int i = 0; !itemRemoved && i < originalList.size(); i++) {
				for (int j = 0; !itemRemoved && j < originalList.size(); j++) {
					if (i != j) {
						if (originalList.get(i).isAncestorOf(originalList.get(j))) {
							originalList.remove(j);
							itemRemoved = true;
						}
						else if (originalList.get(j).isAncestorOf(originalList.get(i))) {
							originalList.remove(i);
							itemRemoved = true;
						}
					}
				}
			}
		} while (itemRemoved);
	}
	
	private boolean Contains(List<OutlineItem> nodes, OutlineItem node) {
		boolean contains = false;
		
		for (OutlineItem currentNode : nodes) {
			if (currentNode.isAncestorOf(node)) {
				contains = true;
				break;
			}
		}
		
		return contains;
	}

	private VaultTreeContentProvider getVaultTreeContentProvider() {
		return (VaultTreeContentProvider) getContentProvider();
	}

	@SuppressWarnings("unchecked")
	public List<OutlineItem> getSelectedItems() {
		ArrayList<OutlineItem> items = new ArrayList<>();
		
		ISelection selection = getSelection();
		
		if (!selection.isEmpty() && selection instanceof IStructuredSelection) {
			IStructuredSelection structuredSelection = (IStructuredSelection) selection;
			
			for (Iterator<OutlineItem> iterator = structuredSelection.iterator(); iterator.hasNext();) {
				OutlineItem node = iterator.next();
				
				// Make sure list of selected nodes does not contain a node multiple times - for example
				// when the node is selected, as well as one if its ancestors.
				if (!Contains(items, node)) {
					items.add(node);
				}
			}
		}
		
		removeRedundantItems(items);
		
		return items;
	}

	/**
	 * Returns a list of all outline items.
	 * @return all outline items
	 */
	public List<OutlineItem> getAllItems() {
		List<TreeItem> allTopLevelItems = getAllTopLevelItems();
		
		List<OutlineItem> allItems = new ArrayList<>();
		
		for (TreeItem treeItem : allTopLevelItems) {
			OutlineItem outlineItem = (OutlineItem) treeItem.getData();
			allItems.add(outlineItem);
		}
		
		return allItems;
	}
	
	private boolean canExpand(TreeItem treeItem) {
		return !TreeHelper.isExpanded(treeItem) && treeItem.getItemCount() > 0;
	}
	
	public boolean canExpandSelectedItems() {
		boolean canExpand = false;
		
		for (TreeItem treeItem : getTree().getSelection()) {
			if (canExpand(treeItem)) {
				canExpand = true;
				break;
			}
		}
		
		return canExpand;
	}
	
	public void expandSelectedItems() {
		if (canExpandSelectedItems()) {
			TreeItem[] selectedItems = getTree().getSelection();
	
			for (TreeItem treeItem : selectedItems) {
				treeItem.setExpanded(true);
			}
	
			for (ITreeViewerListener listener : treeViewerListeners) {
				listener.treeExpanded(null);
			}
			
			refresh();
		}
	}

	public boolean canExpandTreeItem(TreeItem treeItem) {
		boolean canExpand = canExpand(treeItem);
		
		if (!canExpand) {
			for (TreeItem childTreeItem : treeItem.getItems()) {
				if (canExpandTreeItem(childTreeItem)) {
					canExpand = true;
					break;
				}
			}
		}
		
		return canExpand;
	}
	
	public boolean canExpandAllItems() {
		boolean canExpand = false;
		
		List<TreeItem> items = getAllTopLevelItems();
		
		for (TreeItem treeItem : items) {
			if (canExpandTreeItem(treeItem)) {
				canExpand = true;
				break;
			}
		}
		
		return canExpand;
	}
	
	private boolean canCollapse(TreeItem treeItem) {
		return TreeHelper.isExpanded(treeItem);
	}
	
	public boolean canCollapseSelectedItems() {
		boolean canCollapse = false;
		
		for (TreeItem treeItem : getTree().getSelection()) {
			if (canCollapse(treeItem)) {
				canCollapse = true;
				break;
			}
		}
		
		return canCollapse;
	}
	
	public void collapseSelectedItems() {
		if (canCollapseSelectedItems()) {
			TreeItem[] selectedItems = getTree().getSelection();
	
			for (TreeItem treeItem : selectedItems) {
				treeItem.setExpanded(false);
			}
	
			for (ITreeViewerListener listener : treeViewerListeners) {
				listener.treeCollapsed(null);
			}
			
			refresh();
		}
	}
	
	public boolean canCollapseAllItems() {
		boolean canCollapse = false;
		
		List<TreeItem> items = getAllTopLevelItems();

		// Only need to look at top level items because if there is any node that is collapsable,
		// top level items will be collapsable also.
		for (TreeItem childTreeItem : items) {
			if (!canExpand(childTreeItem) && childTreeItem.getItemCount() > 0) {
				canCollapse = true;
				break;
			}
		}
		
		return canCollapse;
	}
	
	public void collapseAllItems() {
		if (canCollapseAllItems()) {
			getTree().setRedraw(false);
			super.collapseAll();
			getTree().setRedraw(true);
	
			for (ITreeViewerListener listener : treeViewerListeners) {
				listener.treeCollapsed(null);
			}
		}
	}

	public boolean canMoveSelectedItems() {
		List<OutlineItem> selectedItems = getSelectedItems();
		
		return selectedItems.size() > 0;
	}

	private void moveSelectedItems(OutlineItem targetNode, boolean targetNodeIsExpanded, boolean addAtTop) {
		if (canMoveSelectedItems()) {
			try {
				Globals.setBusyCursor();
				
				List<OutlineItem> selectedItems = getSelectedItems();
				OutlineItem.move(selectedItems, targetNode, targetNodeIsExpanded, addAtTop);
				Globals.getVaultDocument().setIsModified(true);

				setSelection(new StructuredSelection(selectedItems), true);
			}
			finally {
				Globals.setPreviousCursor();
			}
		}
	}
	
	public void moveSelectedItems(Shell shell) {
		MoveItemsDialog moveItemsDialog = new MoveItemsDialog(shell);

		if (moveItemsDialog.open() == IDialogConstants.OK_ID) {
			UUID selectedNodeUUID = moveItemsDialog.getSelectedNodeUUID();
			OutlineItem rootNode = Globals.getVaultDocument().getContent();
			OutlineItem selectedNode = rootNode.findNode(selectedNodeUUID);

			Globals.getVaultTreeViewer().moveSelectedItems(selectedNode, moveItemsDialog.isSelectedNodeExpanded(), moveItemsDialog.getAddAtTop());
		}
	}
	
	public boolean canMoveUp() {
		boolean result = false;
		
		List<OutlineItem> selectedItems = getSelectedItems();
		
		if (selectedItems.size() == 1) {
			OutlineItem selectedItem = selectedItems.get(0);
			
			int index = selectedItem.getParent().getChildren().indexOf(selectedItem);
			
			result = index > 0; 
		}
		
		return result;
	}
	
	public void moveUp() {
		if (canMoveUp()) {
			List<OutlineItem> selectedItems = getSelectedItems();
			OutlineItem selectedItem = selectedItems.get(0);
			int index = selectedItem.getParent().getChildren().indexOf(selectedItem);
			
			OutlineItem.swap(selectedItem.getParent(), index, index - 1);
			
			setSelection(new StructuredSelection(selectedItem), true);
			
			Globals.getVaultDocument().setIsModified(true);
		}
	}
	
	public boolean canMoveDown() {
		boolean result = false;
		
		List<OutlineItem> selectedItems = getSelectedItems();
		
		if (selectedItems.size() == 1) {
			OutlineItem selectedItem = selectedItems.get(0);
			
			int index = selectedItem.getParent().getChildren().indexOf(selectedItem);
			
			result = index < selectedItem.getParent().getChildren().size() - 1; 
		}
		
		return result;
	}
	
	public void moveDown() {
		if (canMoveDown()) {
			List<OutlineItem> selectedItems = getSelectedItems();
			OutlineItem selectedItem = selectedItems.get(0);
			int index = selectedItem.getParent().getChildren().indexOf(selectedItem);
			
			OutlineItem.swap(selectedItem.getParent(), index, index + 1);
			
			setSelection(new StructuredSelection(selectedItem), true);
			
			Globals.getVaultDocument().setIsModified(true);
		}
	}

	private static boolean itemsAreConsecutiveSiblings(List<OutlineItem> nodes) {
		boolean itemsAreConsecutiveSiblings = nodes.size() > 0;
		
		if (itemsAreConsecutiveSiblings) {
			OutlineItem firstNode = nodes.get(0);
			
			int firstNodeIndex = firstNode.getParent().getChildren().indexOf(firstNode);
			
			for (int i = 1; i < nodes.size(); i++) {
				OutlineItem node = nodes.get(i);
	
				int nodeIndex = firstNode.getParent().getChildren().indexOf(node);
				
				if (nodeIndex - i != firstNodeIndex) {
					itemsAreConsecutiveSiblings = false;
					break;
				}
			}
		}
		
		return itemsAreConsecutiveSiblings;
	}
	
	public boolean canIndentSelectedItems() {
		List<OutlineItem> selectedItems = getSelectedItems();
		
		boolean canIndent = false;
		
		if (selectedItems.size() > 0) {
			OutlineItem firstNode = selectedItems.get(0);
			
			int index = firstNode.getParent().getChildren().indexOf(firstNode);
			
			canIndent = index > 0 && itemsAreConsecutiveSiblings(selectedItems);
		}
		
		return canIndent;
	}
	
	public void indentSelectedItems() {
		if (canIndentSelectedItems()) {
			List<OutlineItem> selectedItems = getSelectedItems();
			OutlineItem.indent(selectedItems);
			Globals.getVaultDocument().setIsModified(true);
			setSelection(new StructuredSelection(selectedItems), true);
		}
	}
	
	public boolean canUnindentSelectedItems() {
		boolean canUnindent = false;
		
		List<OutlineItem> selectedItems = getSelectedItems();

		if (selectedItems.size() > 0) {
			OutlineItem firstNode = selectedItems.get(0);
			
			canUnindent = firstNode.getParent() != null && firstNode.getParent().getParent() != null && itemsAreConsecutiveSiblings(selectedItems);
		}
		
		return canUnindent;
	}
	
	public void unindentSelectedItems() {
		if (canUnindentSelectedItems()) {
			List<OutlineItem> selectedItems = getSelectedItems();
			OutlineItem.unindent(selectedItems);
			Globals.getVaultDocument().setIsModified(true);
			setSelection(new StructuredSelection(selectedItems), true);
		}
	}

	public boolean canRemoveSelectedItems() {
		List<OutlineItem> selectedItems = getSelectedItems();
		
		return selectedItems.size() > 0;
	}
	
	public void removeSelectedItems(boolean promptUserToConfirm) {
		Image icon = Globals.getImageRegistry().get(Globals.IMAGE_REGISTRY_VAULT_ICON);
		
		MessageDialog messageDialog = new MessageDialog(Globals.getMainApplicationWindow().getShell(), StringLiterals.ProgramName, icon, "Remove selected item(s)?", MessageDialog.QUESTION, new String[] { "&Yes", "&No", "&Cancel" }, 0);
		
		if (canRemoveSelectedItems() && (!promptUserToConfirm || messageDialog.open() == 0)) { 
			try {
				Globals.setBusyCursor();
				
				List<OutlineItem> selectedItems = getSelectedItems();
				
				OutlineItem nextOutlineItemToSelect = selectedItems.get(selectedItems.size() - 1).getNextOutlineItemToSelect();
				
				OutlineItem.remove(selectedItems);
				Globals.getVaultDocument().setIsModified(true);
				
				// Remove each item from the content provider's listeners.
				VaultTreeContentProvider vaultTreeContentProvider = (VaultTreeContentProvider) getContentProvider();

				for (OutlineItem nodeToRemove : selectedItems) {
					vaultTreeContentProvider.removeListenerFrom(nodeToRemove);
				}
				
				setSelection(new StructuredSelection(new OutlineItem[] { nextOutlineItemToSelect }));
			}
			finally {
				Globals.setPreviousCursor();
			}
		}
	}

	public boolean canAddItem() {
		List<OutlineItem> selectedItems = getSelectedItems();
		
		return selectedItems.size() <= 1;
	}
	
	public void addItem(OutlineItem newItem, OutlineItem.AddDirection addDirection) {
		if (canAddItem()) {
			IStructuredSelection selection = (IStructuredSelection) getSelection();
			
			OutlineItem node = null;
			boolean nodeIsRoot = false;
			
			if (selection != null) {
				node = (OutlineItem) selection.getFirstElement();
			}
			
			if (node == null) {
				node = Globals.getVaultDocument().getContent();
				nodeIsRoot = true;
			}

			// Need to listen to events generated by this new node.
			VaultTreeContentProvider vaultTreeContentProvider = getVaultTreeContentProvider();
			vaultTreeContentProvider.addListenerTo(newItem);
			
			if (!nodeIsRoot) {
				TreeItem selectedTreeItems[] = getTree().getSelection();
				boolean nodeIsExpanded = TreeHelper.isExpanded(selectedTreeItems[0]);
				
				if (nodeIsExpanded && addDirection == AddDirection.Below) {
					node.addChild(newItem, 0);
				}
				else {
					node.add(newItem, addDirection);
				}
			}
			else {
				node.addChild(newItem, 0);
			}

			Globals.getVaultDocument().setIsModified(true);

			// Select the newly-added node.
			ArrayList<OutlineItem> selectedItems = new ArrayList<>();
			selectedItems.add(newItem);
			setSelection(new StructuredSelection(selectedItems), true);
		}
	}

	public void addItem(Shell shell) {
		boolean showAboveAndBelowRadioButtons = getTree().getItemCount() > 0 && getSelectedItems().size() > 0;
		
		AddItemDialog addItemDialog = new AddItemDialog(shell, showAboveAndBelowRadioButtons, AddItemDialog.Mode.Add, null);
		
		if (addItemDialog.open() == IDialogConstants.OK_ID) {
			OutlineItem newItem = new OutlineItem();
			newItem.setTitle(addItemDialog.getTitle());
			newItem.setText(addItemDialog.getText());
			newItem.setPhotoPath(addItemDialog.getPhotoPath());
			newItem.setAllowScaling(addItemDialog.getAllowScaling());
			
			addItem(newItem, addItemDialog.getAddBelow() ? OutlineItem.AddDirection.Below : OutlineItem.AddDirection.Above);
		}
	}
	
	public boolean canCutSelectedItems() {
		List<OutlineItem> selectedItems = getSelectedItems();
		
		return selectedItems.size() > 0;
	}
	
	public void cutSelectedItems() {
		if (canCutSelectedItems()) {
			copySelectedItems();
			removeSelectedItems(false);
		}
	}
	
	public boolean canCopySelectedItems() {
		List<OutlineItem> selectedItems = getSelectedItems();
		
		return selectedItems.size() > 0;
	}
	
	public void copySelectedItems() {
		if (canCopySelectedItems()) {
			List<OutlineItem> selectedItems = getSelectedItems();
			Globals.getClipboard().replaceItems(selectedItems);
		}
	}
	
	public boolean canPasteItems() {
		List<OutlineItem> selectedItems = getSelectedItems();
		
		return selectedItems.size() == 1 && !Globals.getClipboard().isEmpty();
	}
	
	private void pasteItems(boolean addAtTop) {
		if (canPasteItems()) {
			try {
				Globals.setBusyCursor();
				
				IStructuredSelection selection = (IStructuredSelection) getSelection();
				OutlineItem targetNode = (OutlineItem) selection.getFirstElement();

				TreeItem selectedTreeItems[] = getTree().getSelection();
				final boolean targetNodeIsExpanded = TreeHelper.isExpanded(selectedTreeItems[0]);
				
				List<OutlineItem> selectedItems = Globals.getClipboard().getItems();
				
				// Need to listen to events generated by this new node.
				VaultTreeContentProvider vaultTreeContentProvider = getVaultTreeContentProvider();
				
				for (OutlineItem item : selectedItems) {
					vaultTreeContentProvider.addListenerTo(item);
				}
				
				OutlineItem.paste(selectedItems, targetNode, targetNodeIsExpanded, addAtTop);
				Globals.getVaultDocument().setIsModified(true);

				setSelection(new StructuredSelection(selectedItems), true);
			}
			finally {
				Globals.setPreviousCursor();
			}
		}
	}
	
	public void pasteItems(Shell shell) {
		List<OutlineItem> selectedItems = Globals.getVaultTreeViewer().getSelectedItems();
		OutlineItem selectedNode = selectedItems.get(0);
		
		boolean addAtTop = false;
		boolean cancel = false;
		
		// If selected node is first item, find out if user wants the items to be moved to the top.
		if (Globals.getVaultDocument().getContent().getChildren().indexOf(selectedNode) == 0) {
			PasteAboveOrBelowDialog pasteAboveOrBelowDialog = new PasteAboveOrBelowDialog(shell);
			pasteAboveOrBelowDialog.open();
			cancel = pasteAboveOrBelowDialog.isCancelled();
			addAtTop = pasteAboveOrBelowDialog.getAddAbove();
		}

		if (!cancel) {
			Globals.getVaultTreeViewer().pasteItems(addAtTop);
		}
	}

	public boolean canEditOutlineItem() {
		List<OutlineItem> selectedItems = getSelectedItems();
		
		return selectedItems.size() == 1;
	}
	
	/**
	 * Allow the user to change the Title, Text, or photo file attachment of the selected item.
	 * @param shell Shell
	 */
	public void editOutlineItem(Shell shell) {
		if (canEditOutlineItem()) {
			List<OutlineItem> selectedItems = getSelectedItems();

			OutlineItem selectedItem = selectedItems.get(0);
			
			AddItemDialog addItemDialog = new AddItemDialog(shell,false, AddItemDialog.Mode.Edit, selectedItem); 
			
			if (addItemDialog.open() == IDialogConstants.OK_ID) {
				if (!addItemDialog.getTitle().equals(selectedItem.getTitle()) || 
					!addItemDialog.getText().equals(selectedItem.getText()) || 
					!addItemDialog.getAllowScaling() == selectedItem.getAllowScaling() ||
					!StringUtils.equals(addItemDialog.getPhotoPath(), selectedItem.getPhotoPath())) {
					selectedItem.setTitle(addItemDialog.getTitle());
					selectedItem.setText(addItemDialog.getText());
					selectedItem.setPhotoPath(addItemDialog.getPhotoPath());
					selectedItem.setAllowScaling(addItemDialog.getAllowScaling());
					
					// Force text changes to take effect.
					Globals.getVaultTextViewer().setOutlineItem(selectedItem);
					
					Globals.getMainApplicationWindow().getPhotoAndTextUI().getPhotoUI().setAllowScaling(selectedItem.getAllowScaling());
					Globals.getMainApplicationWindow().getPhotoAndTextUI().getPhotoUI().setImages(selectedItem.getPhotoPath());
					
					// Render or hide photo as appropriate.
					Globals.getMainApplicationWindow().getPhotoAndTextUI().selectionChanged();
					
					Globals.getVaultDocument().setIsModified(true);
				}
			}
		}
	}
	
	/**
	 * Determine if the selected outline items can be sorted
	 * @return true if the items can be sorted
	 */
	public boolean canSort() {
		List<OutlineItem> selectedItems = getSelectedItems();
		
		boolean moreThanOneItem;
		
		if (selectedItems.size() == 1) {
			moreThanOneItem = selectedItems.get(0).getChildren().size() > 1;
		}
		else {
			moreThanOneItem = selectedItems.size() > 1;
		}
		
		return moreThanOneItem && itemsAreConsecutiveSiblings(selectedItems);
	}
	
	/**
	 * Sort the selected outline items
	 */
	public void sort() {
		if (canSort()) {
			try {
				Globals.setBusyCursor();

				boolean itemsMoved = false;
				
				List<OutlineItem> selectedItems = getSelectedItems();
				
				if (selectedItems.size() == 1) {
					itemsMoved = OutlineItem.sort(selectedItems.get(0).getChildren());
				}
				else if (selectedItems.size() > 1) {
					itemsMoved = OutlineItem.sort(selectedItems);
				}
				
				if (itemsMoved) {
					Globals.getVaultDocument().setIsModified(true);
				}
			}
			finally {
				Globals.setPreviousCursor();
			}
		}
	}

	/**
	 * Make the current document an empty document
	 */
	public void fileNew() {
		VaultDocument vaultDocument = new VaultDocument();
		Globals.setVaultDocument(vaultDocument);
		load(Globals.getVaultDocument().getContent());
	}
	
	/**
	 * Change the Outline's font
	 */
	public void setFont() {
		FontDialog fontDialog = new FontDialog(getTree().getShell());
		fontDialog.setText("Set Font");
		
		fontDialog.setRGB(getTree().getForeground().getRGB());

		fontDialog.setFontList(getTree().getFont().getFontData());
		
		FontData fontData = fontDialog.open();
		
		if (fontData != null) {
			String fontString = FontUtils.fontListToString(fontDialog.getFontList());
			Globals.getPreferenceStore().setValue(PreferenceKeys.OutlineFontString, fontString);
			Globals.getPreferenceStore().setValue(PreferenceKeys.OutlineFontRed,   fontDialog.getRGB().red);
			Globals.getPreferenceStore().setValue(PreferenceKeys.OutlineFontGreen, fontDialog.getRGB().green);
			Globals.getPreferenceStore().setValue(PreferenceKeys.OutlineFontBlue,  fontDialog.getRGB().blue);
		
			applyUserPreferences();
		}
	}

	public boolean canSearch() {
		return getAllTopLevelItems().size() > 0;
	}

	@Override
	public void addTreeListener(ITreeViewerListener listener) {
		super.addTreeListener(listener);
		treeViewerListeners.add(listener);
	}

	@Override
	protected void addTreeListener(Control c, TreeListener listener) {
		super.addTreeListener(c, listener);
	}

	public List<TreeItem> getAllTopLevelItems() {
		ArrayList<TreeItem> allTopLevelItems = new ArrayList<>();

		Collections.addAll(allTopLevelItems, getTree().getItems());
				
		return allTopLevelItems;
	}
}