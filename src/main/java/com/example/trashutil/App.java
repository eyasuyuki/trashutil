package com.example.trashutil;

import java.awt.BorderLayout;
import java.awt.EventQueue;
import java.awt.datatransfer.DataFlavor;
import java.awt.datatransfer.Transferable;
import java.awt.dnd.DnDConstants;
import java.awt.dnd.DropTarget;
import java.awt.dnd.DropTargetDropEvent;
import java.awt.event.ActionEvent;
import java.awt.event.ActionListener;
import java.io.File;
import java.util.ArrayList;
import java.util.List;
import java.util.prefs.Preferences;

import javax.swing.GroupLayout;
import javax.swing.GroupLayout.Alignment;
import javax.swing.JButton;
import javax.swing.JFileChooser;
import javax.swing.JFrame;
import javax.swing.JLabel;
import javax.swing.JPanel;
import javax.swing.JTextArea;
import javax.swing.JTextField;
import javax.swing.JToolBar;
import javax.swing.LayoutStyle.ComponentPlacement;
import javax.swing.border.EmptyBorder;
import javax.swing.event.DocumentEvent;
import javax.swing.event.DocumentListener;

import org.apache.commons.io.FilenameUtils;
import org.apache.tools.ant.DirectoryScanner;

import com.sun.jna.platform.win32.W32FileUtils;

//import com.drew.metadata.MetadataException;

public class App extends JFrame {

	/**
	 * 
	 */
	private static final long serialVersionUID = 1L;
	private static final String NEWLINE = System.getProperty("line.separator");
	private static final Preferences prefs = Preferences.userNodeForPackage(App.class);
	private static final String SRC_KEY = "src";
	private static final String DEST_KEY = "dest";
	//private static final String MOVE_KEY = "move";
	private JPanel contentPane;
	private JButton copyButton;
	private JTextField srcField;
	private JTextField destField;
	private JTextArea textArea;
	private Object mutex = new Object();
	private boolean stop = false;
	
	public void setStop(boolean stop) {
		synchronized (mutex) {
			this.stop = stop;
		}
	}
	
	public boolean isStop() {
		return stop;
	}

	/**
	 * Launch the application.
	 */
	public static void main(String[] args) {
		EventQueue.invokeLater(new Runnable() {
			public void run() {
				try {
					App frame = new App();
					frame.setVisible(true);
				} catch (Exception e) {
					e.printStackTrace();
				}
			}
		});
	}

	void chooseDirectory(JTextField field) {
		String prev = field.getText();
		JFileChooser chooser = null;
		if (prev != null && prev.length() > 0) {
			chooser = new JFileChooser(new File(prev));
		} else {
			chooser = new JFileChooser();
		}
		chooser.setFileSelectionMode(JFileChooser.DIRECTORIES_ONLY);
		if (chooser.showOpenDialog(App.this) == JFileChooser.APPROVE_OPTION) {
			File dir = chooser.getSelectedFile();
			field.setText(dir.getAbsolutePath());
		}
	}
	
	void doDrop(DropTargetDropEvent dtde, JTextField field) {
		Transferable t = dtde.getTransferable();
		if (t.isDataFlavorSupported(DataFlavor.javaFileListFlavor)) {
			dtde.acceptDrop(DnDConstants.ACTION_REFERENCE);
			try {
				List list = (List)t.getTransferData(DataFlavor.javaFileListFlavor);
				for (int i = 0; i < list.size(); i++) {
					File f = (File) list.get(i);
					if (f.isDirectory()) {
						field.setText(f.getAbsolutePath());
						break;
					}
				}
			} catch (Exception e) {
				e.printStackTrace();
			}
		}
	}
	
	DropTarget srcTarget = new DropTarget() {
		@Override
		public synchronized void drop(DropTargetDropEvent dtde) {
			doDrop(dtde, srcField);
		}};
		
	DropTarget destTarget = new DropTarget() {
		@Override
		public synchronized void drop(DropTargetDropEvent dtde) {
			doDrop(dtde, destField);
		}};
		
//	void retrieve(File dir, File destRoot,
//			Exifer exifer, boolean recursive, boolean setExifDate,
//			boolean forceCopy, boolean move) throws MetadataException {
//		File[] files = dir.listFiles();
//
//		if (files == null || files.length <= 0) return;
//		for (File f: files) {
//			if (isStop()) break;
//			if (f.isFile()) {
//				exifer.copyExif(f, destRoot, setExifDate, forceCopy, move);
//			}
//			if (f.isDirectory() && recursive) {
//				retrieve(f, destRoot, exifer, recursive, setExifDate, forceCopy, move);
//			}
//		}
//	}

	/**
	 * Create the frame.
	 */
	public App() {
		setDefaultCloseOperation(JFrame.EXIT_ON_CLOSE);
		setBounds(100, 100, 800, 600);
		contentPane = new JPanel();
		contentPane.setBorder(new EmptyBorder(5, 5, 5, 5));
		contentPane.setLayout(new BorderLayout(0, 0));
		setContentPane(contentPane);
		
		JToolBar toolBar = new JToolBar();
		contentPane.add(toolBar, BorderLayout.NORTH);

		copyButton = new JButton("Move to trash");
		copyButton.addActionListener(new ActionListener() {
			public void actionPerformed(ActionEvent e) {
				stop = false;
				Thread thread = new Thread(new Runnable(){
					@Override
					public void run() {
						List<String> names = new ArrayList<String>();
						List<File> goToTrash = new ArrayList<File>();
						String path = FilenameUtils.getFullPath(srcField.getText());
						String name = FilenameUtils.getName(srcField.getText());
						String dest = FilenameUtils.getFullPath(destField.getText());
						try {
							textArea.setText("");
							DirectoryScanner ds = new DirectoryScanner();
							ds.setIncludes(new String[]{"**/"+name});
							ds.setBasedir(path);
							ds.scan();
							String files[] = ds.getIncludedFiles();
							for (int i=0; i<files.length; i++) {
								names.add(FilenameUtils.getName(files[i]));
							}
							ds.setBasedir(dest);
							ds.scan();
							files = ds.getIncludedFiles();
							for (String d: files) {
								String n = FilenameUtils.getName(d);
								if (names.contains(n)) {
									goToTrash.add(new File(d));
								}
							}
							W32FileUtils.getInstance().moveToTrash(goToTrash.toArray(new File[0]));
						} catch (Exception e1) {
							e1.printStackTrace();
						}
					}});
				thread.start();
			}
		});
		toolBar.add(copyButton);
		
		JButton stopButton = new JButton("Stop");
		stopButton.addActionListener(new ActionListener() {
			public void actionPerformed(ActionEvent e) {
				setStop(true);
			}
		});
		toolBar.add(stopButton);
		
		JPanel panel = new JPanel();
		contentPane.add(panel, BorderLayout.CENTER);
		
		srcField = new JTextField();
		srcField.setColumns(10);
		srcField.setDropTarget(srcTarget);
		srcField.setText(prefs.get(SRC_KEY, ""));
		srcField.getDocument().addDocumentListener(new DocumentListener(){

			@Override
			public void insertUpdate(DocumentEvent e) {
				prefs.put(SRC_KEY, srcField.getText());
			}

			@Override
			public void removeUpdate(DocumentEvent e) {
				prefs.put(SRC_KEY, srcField.getText());
			}

			@Override
			public void changedUpdate(DocumentEvent e) {
				prefs.put(SRC_KEY, srcField.getText());
			}});
		
		destField = new JTextField();
		destField.setColumns(10);
		destField.setDropTarget(destTarget);
		destField.setText(prefs.get(DEST_KEY, ""));
		destField.getDocument().addDocumentListener(new DocumentListener(){

			@Override
			public void insertUpdate(DocumentEvent e) {
				prefs.put(DEST_KEY, destField.getText());
			}

			@Override
			public void removeUpdate(DocumentEvent e) {
				prefs.put(DEST_KEY, destField.getText());
			}

			@Override
			public void changedUpdate(DocumentEvent e) {
				prefs.put(DEST_KEY, destField.getText());
			}});
		
		JButton srcButton = new JButton("File...");
		srcButton.addActionListener(new ActionListener() {
			public void actionPerformed(ActionEvent e) {
				chooseDirectory(srcField);
			}
		});
		
		JButton destButton = new JButton("File...");
		destButton.addActionListener(new ActionListener() {
			public void actionPerformed(ActionEvent e) {
				chooseDirectory(destField);
			}
		});
		
		textArea = new JTextArea();
		
		JLabel lblFrom = new JLabel("Scan:");
		
		JLabel lblTo = new JLabel("Delete:");
		
		JLabel lblLog = new JLabel("Log:");
		GroupLayout gl_panel = new GroupLayout(panel);
		gl_panel.setHorizontalGroup(
			gl_panel.createParallelGroup(Alignment.TRAILING)
				.addGroup(gl_panel.createSequentialGroup()
					.addContainerGap()
					.addGroup(gl_panel.createParallelGroup(Alignment.TRAILING)
						.addComponent(lblFrom)
						.addComponent(lblTo)
						.addComponent(lblLog))
					.addPreferredGap(ComponentPlacement.RELATED)
					.addGroup(gl_panel.createParallelGroup(Alignment.TRAILING)
						.addComponent(textArea, Alignment.LEADING, GroupLayout.DEFAULT_SIZE, 361, Short.MAX_VALUE)
						.addGroup(gl_panel.createSequentialGroup()
							.addGroup(gl_panel.createParallelGroup(Alignment.LEADING)
								.addComponent(destField, GroupLayout.DEFAULT_SIZE, 271, Short.MAX_VALUE)
								.addComponent(srcField, GroupLayout.DEFAULT_SIZE, 271, Short.MAX_VALUE))
							.addGap(12)
							.addGroup(gl_panel.createParallelGroup(Alignment.TRAILING)
								.addComponent(destButton, GroupLayout.PREFERRED_SIZE, 78, GroupLayout.PREFERRED_SIZE)
								.addComponent(srcButton))))
					.addContainerGap())
		);
		gl_panel.setVerticalGroup(
			gl_panel.createParallelGroup(Alignment.LEADING)
				.addGroup(gl_panel.createSequentialGroup()
					.addContainerGap()
					.addGroup(gl_panel.createParallelGroup(Alignment.BASELINE)
						.addComponent(srcField, GroupLayout.PREFERRED_SIZE, GroupLayout.DEFAULT_SIZE, GroupLayout.PREFERRED_SIZE)
						.addComponent(srcButton)
						.addComponent(lblFrom))
					.addPreferredGap(ComponentPlacement.RELATED)
					.addGroup(gl_panel.createParallelGroup(Alignment.BASELINE)
						.addComponent(destField, GroupLayout.PREFERRED_SIZE, GroupLayout.DEFAULT_SIZE, GroupLayout.PREFERRED_SIZE)
						.addComponent(destButton)
						.addComponent(lblTo))
					.addPreferredGap(ComponentPlacement.RELATED)
					.addGroup(gl_panel.createParallelGroup(Alignment.BASELINE)
						.addComponent(textArea, GroupLayout.DEFAULT_SIZE, 162, Short.MAX_VALUE)
						.addComponent(lblLog))
					.addContainerGap())
		);
		panel.setLayout(gl_panel);
	}
}
