package de.mpicbg.knime.scripting.core;

import javax.swing.*;
import javax.swing.event.UndoableEditEvent;
import javax.swing.event.UndoableEditListener;
import javax.swing.text.*;
import javax.swing.undo.AbstractUndoableEdit;
import javax.swing.undo.CannotUndoException;
import javax.swing.undo.CompoundEdit;
import javax.swing.undo.UndoableEdit;
import java.awt.*;
import java.awt.event.ActionEvent;
import java.awt.event.ActionListener;
import java.util.ArrayList;
import java.util.HashMap;


public class UndoableEditPane extends JEditorPane {

    UndoManager undoManager;
    public JButton undoButton = new JButton("Undo");
    public JButton redoButton = new JButton("Redo");


    public static void main(String[] args) {
        JFrame frame = new JFrame("Merge undoable actions in one group");

        frame.setDefaultCloseOperation(JFrame.EXIT_ON_CLOSE);


        final UndoableEditPane app = new UndoableEditPane();

        JScrollPane scroll = new JScrollPane(app);
        frame.getContentPane().add(scroll);

        JToolBar tb = app.createToolBar();
        frame.getContentPane().add(tb, BorderLayout.NORTH);

        frame.setSize(400, 200);
        frame.setLocationRelativeTo(null);
        frame.setVisible(true);
    }


    public void resetHistory() {
        undoManager = new UndoManager();

        getDocument().addUndoableEditListener(undoManager);

        undoManager.refreshControls();
    }


    public JToolBar createToolBar() {
        JToolBar toolBar = new JToolBar();


        HashMap<Object, Action> actions = createActionTable(this);


        JButton copyButton = new JButton();
        copyButton.setAction(actions.get(DefaultEditorKit.copyAction));
        copyButton.setText("Copy");
        toolBar.add(copyButton);


        JButton cutButton = new JButton();
        cutButton.setAction(actions.get(DefaultEditorKit.cutAction));
        cutButton.setText("Cut");
        toolBar.add(cutButton);


        JButton pasteButton = new JButton();
        pasteButton.setAction(actions.get(DefaultEditorKit.pasteAction));
        pasteButton.setText("Paste");
        toolBar.add(pasteButton);

        toolBar.addSeparator();

        undoButton.addActionListener(new ActionListener() {
            public void actionPerformed(ActionEvent e) {
                undoManager.undo();
            }
        });
        toolBar.add(undoButton);

        redoButton.addActionListener(new ActionListener() {
            public void actionPerformed(ActionEvent e) {
                undoManager.redo();
            }
        });
        toolBar.add(redoButton);


        return toolBar;
    }


    private static HashMap<Object, Action> createActionTable(JTextComponent textComponent) {
        HashMap<Object, Action> actions = new HashMap<Object, Action>();
        Action[] actionsArray = textComponent.getActions();
        for (int i = 0; i < actionsArray.length; i++) {
            Action a = actionsArray[i];
            actions.put(a.getValue(Action.NAME), a);
        }
        return actions;
    }


    public UndoableEditPane() {
        super();
        setEditorKit(new StyledEditorKit());
        resetHistory();
    }


    class MyCompoundEdit extends CompoundEdit {

        boolean isUnDone = false;


        public int getLength() {
            return edits.size();
        }

//        public String getLast(){
//            if(edits.isEmpty()) {
//                return null;
//            }
//            edits.get(edits.size()).
//        }


        public void undo() throws CannotUndoException {
            super.undo();
            isUnDone = true;
        }


        public void redo() throws CannotUndoException {
            super.redo();
            isUnDone = false;
        }


        public boolean canUndo() {
            return edits.size() > 0 && !isUnDone;
        }


        public boolean canRedo() {
            return edits.size() > 0 && isUnDone;
        }

    }


    class UndoManager extends AbstractUndoableEdit implements UndoableEditListener {

        String lastEditName = null;
        ArrayList<MyCompoundEdit> edits = new ArrayList<MyCompoundEdit>();
        MyCompoundEdit current;
        int pointer = -1;


        public void undoableEditHappened(UndoableEditEvent e) {
            UndoableEdit edit = e.getEdit();
            if (edit instanceof AbstractDocument.DefaultDocumentEvent) {
                try {
                    AbstractDocument.DefaultDocumentEvent event = (AbstractDocument.DefaultDocumentEvent) edit;

                    boolean isNeedStart = false;

                    if (edit.getPresentationName().equals("deletion")) {
                        isNeedStart = true;
                    } else {
                        int start = event.getOffset();
                        int len = event.getLength();
                        String text = event.getDocument().getText(start, len);
                        if (current == null) {
                            isNeedStart = true;
                        } else if (current.getLength() > 1 && text.equals(" ")) {
                            isNeedStart = true;
                        } else if (text.contains("\n")) {
                            isNeedStart = true;

                        } else if (lastEditName == null || !lastEditName.equals(edit.getPresentationName())) {
                            isNeedStart = true;
                        }

                        while (pointer < edits.size() - 1) {
                            edits.remove(edits.size() - 1);
                            isNeedStart = true;
                        }
                    }
                    if (isNeedStart) {
                        createCompoundEdit();
                    }

                    current.addEdit(edit);
                    lastEditName = edit.getPresentationName();

                    refreshControls();
                } catch (BadLocationException e1) {
                    e1.printStackTrace();
                }
            }
        }


        public void createCompoundEdit() {
            if (current == null) {
                current = new MyCompoundEdit();
            } else if (current.getLength() > 0) {
                current = new MyCompoundEdit();
            }

            edits.add(current);
            pointer++;
        }


        public void undo() throws CannotUndoException {
            if (!canUndo()) {
//                return;
                throw new CannotUndoException();
            }

            MyCompoundEdit u = edits.get(pointer);
            u.undo();
            pointer--;

            refreshControls();
        }


        public void redo() throws CannotUndoException {
            if (!canRedo()) {
//                return;
                throw new CannotUndoException();
            }

            pointer++;
            MyCompoundEdit u = edits.get(pointer);
            u.redo();

            refreshControls();
        }


        public boolean canUndo() {
            return pointer >= 0;
        }


        public boolean canRedo() {
            return edits.size() > 0 && pointer < edits.size() - 1;
        }


        public void refreshControls() {
            undoButton.setEnabled(canUndo());
            redoButton.setEnabled(canRedo());
        }
    }
}
