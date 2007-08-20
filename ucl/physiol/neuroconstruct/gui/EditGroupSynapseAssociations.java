/**
 * neuroConstruct
 *
 * Software for developing large scale 3D networks of biologically realistic neurons
 * Copyright (c) 2007 Padraig Gleeson
 * UCL Department of Physiology
 *
 * Development of this software was made possible with funding from the
 * Medical Research Council
 *
 */

package ucl.physiol.neuroconstruct.gui;

import java.awt.*;
import java.awt.event.*;
import javax.swing.*;

import ucl.physiol.neuroconstruct.utils.*;
import ucl.physiol.neuroconstruct.cell.*;
import java.util.*;
import javax.swing.border.*;
import javax.swing.event.*;
import ucl.physiol.neuroconstruct.cell.examples.*;
import ucl.physiol.neuroconstruct.cell.utils.*;

/**
 * Dialog for editing which synapses are associated with which groups
 *
 * @author Padraig Gleeson
 * @version 1.0.6
 */

public class EditGroupSynapseAssociations extends JDialog implements ListSelectionListener
{
    ClassLogger logger = new ClassLogger("EditGroupSynapseAssociations");
    public boolean cancelled = false;

    private DefaultListModel listModelGroupsIn = new DefaultListModel();
    private DefaultListModel listModelGroupsOut = new DefaultListModel();

    String defaultProcessSelection = "-- Please select --";

    Frame myParent = null;

    Vector processOptions = null;

    String processType = null;

    //UpdateOneCell updateInterface = null;

    Cell myCell = null;

    JPanel jPanelMain = new JPanel();
    JPanel jPanelButtons = new JPanel();
    JPanel jPanelSelectProcess = new JPanel();
    JPanel jPanelLists = new JPanel();
    JLabel jLabelSelect = new JLabel();
    JComboBox jComboBoxProcessNames = new JComboBox();
    JList jListGroupsOut = new JList(listModelGroupsOut);
    JList jListGroupsIn = new JList(listModelGroupsIn);

    JScrollPane scrollPaneSectionsOut = new JScrollPane(jListGroupsOut);
    JScrollPane scrollPaneSectionsIn = new JScrollPane(jListGroupsIn);

    //JViewport viewportSectionsOut = scrollPaneSectionsOut.getViewport();
    //JViewport viewportSectionsIn = scrollPaneSectionsIn.getViewport();

    JPanel jPanelGroupsIn = new JPanel();
    JPanel jPanelSwitch = new JPanel();
    JPanel jPanelGroupsOut = new JPanel();
    JButton jButtonAdd = new JButton();
    JButton jButtonRemove = new JButton();
    GridBagLayout gridBagLayout1 = new GridBagLayout();
    JLabel jLabelGroupsOut = new JLabel();
    BorderLayout borderLayout2 = new BorderLayout();
    Border border1;
    Border border2;
    JLabel jLabelGroupsIn = new JLabel();
    BorderLayout borderLayout3 = new BorderLayout();
    Border border3;
    Border border4;
    GridBagLayout gridBagLayout2 = new GridBagLayout();
    JButton jButtonOK = new JButton();
    GridBagLayout gridBagLayout3 = new GridBagLayout();
    Border border5;

    private EditGroupSynapseAssociations()
    {

    }

    public EditGroupSynapseAssociations(Cell cell,
                                        Frame owner,
                                        String processType,
                                        Vector processList) throws HeadlessException
    {
        super(owner, "Edit Group to "+processType + " associations", true);

        processOptions = processList;

        //updateInterface = update;
        this.processType = processType;

        myCell = cell;


        ArrayList<String> processes = cell.getAllAllowedSynapseTypes();
        for (int i = 0; i < processes.size(); i++)
        {
            String nextProcess = processes.get(i);
            if (!processList.contains(nextProcess))
            {
                int result = JOptionPane.showConfirmDialog(this, "The synapse type " +
                                                           nextProcess +
                                                           " is present on this cell, but there is no corresponding "
                                                           +
                                                           "\nCell Process in the project. Delete the synapse from the cell?",
                                                           "Warning",
                                                           JOptionPane.YES_NO_OPTION);

                if (result == JOptionPane.YES_OPTION)
                {
                    logger.logComment("Deleting...");
                    Vector groups = cell.getGroupsWithSynapse(nextProcess);
                    int numPresent = groups.size();

                    for (int k = 0; k < numPresent; k++)
                    {
                        String nextGroup = (String) groups.elementAt(k);
                        logger.logComment("Removign syn from group: "+ nextGroup);
                        logger.logComment("Success? " + cell.disassociateGroupFromSynapse(nextGroup, nextProcess));

                    }
                    //cell.
                }
                else logger.logComment("Leaving io alone...");

                logger.logComment("Details: " + CellTopologyHelper.printDetails(cell, null));
            }
        }


        myParent = owner;
        try
        {
            jbInit();
            extraInit();
            pack();
        }
        catch (Exception ex)
        {
            logger.logComment("Exception starting GUI: "+ ex);
        }
    }

    private void jbInit() throws Exception
    {
        border1 = BorderFactory.createCompoundBorder(BorderFactory.createBevelBorder(BevelBorder.LOWERED, Color.white,
            Color.white, new Color(124, 124, 124), new Color(178, 178, 178)),
                                                     BorderFactory.createEmptyBorder(3, 3, 3, 3));
        border2 = BorderFactory.createEmptyBorder(5,5,5,5);
        border3 = BorderFactory.createCompoundBorder(BorderFactory.createBevelBorder(BevelBorder.LOWERED, Color.white,
            Color.white, new Color(124, 124, 124), new Color(178, 178, 178)),
                                                     BorderFactory.createEmptyBorder(3, 3, 3, 3));
        border4 = BorderFactory.createEmptyBorder(5,5,5,5);
        border5 = BorderFactory.createEmptyBorder(5,5,5,5);
        jPanelMain.setLayout(gridBagLayout3);


        jLabelSelect.setText("Please select a synapse type:");
        jPanelLists.setLayout(gridBagLayout2);
        jPanelGroupsOut.setBorder(border2);
        jPanelGroupsOut.setMaximumSize(new Dimension(160, 129));
        jPanelGroupsOut.setMinimumSize(new Dimension(160, 129));
        jPanelGroupsOut.setPreferredSize(new Dimension(160, 129));
        jPanelGroupsOut.setLayout(borderLayout2);
        jPanelGroupsIn.setBorder(border4);
        jPanelGroupsIn.setMaximumSize(new Dimension(160, 129));
        jPanelGroupsIn.setMinimumSize(new Dimension(160, 129));
        jPanelGroupsIn.setPreferredSize(new Dimension(160, 129));
        jPanelGroupsIn.setLayout(borderLayout3);
        jButtonAdd.setText(">");
        jButtonAdd.addActionListener(new java.awt.event.ActionListener()
        {
            public void actionPerformed(ActionEvent e)
            {
                jButtonAdd_actionPerformed(e);
            }
        });
        jButtonRemove.setText("<");
        jButtonRemove.addActionListener(new java.awt.event.ActionListener()
        {
            public void actionPerformed(ActionEvent e)
            {
                jButtonRemove_actionPerformed(e);
            }
        });
        jPanelSwitch.setLayout(gridBagLayout1);
        jLabelGroupsOut.setHorizontalAlignment(SwingConstants.CENTER);
        jLabelGroupsOut.setText("Groups without "+ processType);
        jListGroupsOut.setBorder(border1);
        jListGroupsOut.setMaximumSize(new Dimension(100, 100));
        jListGroupsOut.setMinimumSize(new Dimension(100, 100));
        jListGroupsOut.setPreferredSize(new Dimension(100, 100));
        jLabelGroupsIn.setHorizontalAlignment(SwingConstants.CENTER);
        jLabelGroupsIn.setText("Groups with "+ processType);
        jListGroupsIn.setBorder(border3);

        scrollPaneSectionsOut.setVerticalScrollBarPolicy(JScrollPane.VERTICAL_SCROLLBAR_ALWAYS);
        scrollPaneSectionsOut.setMaximumSize(new Dimension(200, 30));
        scrollPaneSectionsOut.setMinimumSize(new Dimension(200, 30));
        scrollPaneSectionsOut.setPreferredSize(new Dimension(200, 30));
        scrollPaneSectionsIn.setVerticalScrollBarPolicy(JScrollPane.VERTICAL_SCROLLBAR_ALWAYS);
        scrollPaneSectionsIn.setMaximumSize(new Dimension(200, 100));
        scrollPaneSectionsIn.setMinimumSize(new Dimension(200, 100));
        scrollPaneSectionsIn.setPreferredSize(new Dimension(200, 100));
        jComboBoxProcessNames.addItemListener(new java.awt.event.ItemListener()
        {
            public void itemStateChanged(ItemEvent e)
            {
                jComboBoxGroupNames_itemStateChanged(e);
            }
        });
        jButtonOK.setText("OK");
        jButtonOK.addActionListener(new java.awt.event.ActionListener()
        {
            public void actionPerformed(ActionEvent e)
            {
                jButtonOK_actionPerformed(e);
            }
        });


        jPanelSwitch.setBorder(null);
        jPanelSwitch.setMinimumSize(new Dimension(30, 30));
        jPanelSwitch.setPreferredSize(new Dimension(30, 30));

        jPanelLists.setBorder(border5);
        jPanelLists.setMaximumSize(new Dimension(410, 284));
        jPanelLists.setMinimumSize(new Dimension(410, 284));
        jPanelLists.setPreferredSize(new Dimension(410, 284));

        jPanelMain.setMaximumSize(new Dimension(470, 370));
        jPanelMain.setMinimumSize(new Dimension(470, 370));
        jPanelMain.setPreferredSize(new Dimension(470, 370));

        this.getContentPane().add(jPanelMain, BorderLayout.CENTER);
        jPanelMain.add(jPanelSelectProcess,   new GridBagConstraints(0, 0, 1, 1, 1.0, 1.0
            ,GridBagConstraints.CENTER, GridBagConstraints.NONE, new Insets(0, 0, 0, 0), 0, 0));
        jPanelMain.add(jPanelLists,    new GridBagConstraints(0, 1, 1, 1, 1.0, 1.0
            ,GridBagConstraints.CENTER, GridBagConstraints.NONE, new Insets(0, 0, 0, 0), 100, 0));
        jPanelLists.add(jPanelGroupsOut, new GridBagConstraints(0, 0, 1, 1, 1.0, 1.0
                                                                  , GridBagConstraints.CENTER,
                                                                  GridBagConstraints.VERTICAL, new Insets(0, 0, 0, 0),
                                                                  70, 0));
        jPanelLists.add(jPanelSwitch,  new GridBagConstraints(1, 0, 1, 1, 1.0, 1.0
            ,GridBagConstraints.CENTER, GridBagConstraints.NONE, new Insets(0, 0, 0, 0), 40, 254));
        jPanelSwitch.add(jButtonAdd, new GridBagConstraints(0, 0, 1, 1, 0.0, 0.0
                                                            , GridBagConstraints.CENTER, GridBagConstraints.NONE,
                                                            new Insets(0, 0, 12, 0), 0, 0));
        jPanelLists.add(jPanelGroupsIn, new GridBagConstraints(2, 0, 1, 1, 1.0, 1.0
                                                                 , GridBagConstraints.CENTER,
                                                                 GridBagConstraints.VERTICAL, new Insets(0, 0, 0, 1),
                                                                 70, 0));
        jPanelMain.add(jPanelButtons,  new GridBagConstraints(0, 2, 1, 1, 1.0, 1.0
            ,GridBagConstraints.CENTER, GridBagConstraints.NONE, new Insets(0, 0, 1, 0), 0, 0));
        jPanelButtons.add(jButtonOK, null);
        jPanelSelectProcess.add(jLabelSelect, null);
        jPanelSelectProcess.add(jComboBoxProcessNames, null);

        jPanelSwitch.add(jButtonRemove, new GridBagConstraints(0, 1, 1, 1, 0.0, 0.0
                                                               , GridBagConstraints.CENTER, GridBagConstraints.NONE,
                                                               new Insets(0, 0, 0, 1), 0, 0));

        jPanelGroupsOut.add(jLabelGroupsOut, BorderLayout.NORTH);
        jPanelGroupsOut.add(scrollPaneSectionsOut, BorderLayout.CENTER);
        //viewportSectionsOut.setView(jListSectionsOut);

        jPanelGroupsIn.add(jLabelGroupsIn, BorderLayout.NORTH);
        jPanelGroupsIn.add(scrollPaneSectionsIn, BorderLayout.CENTER);
        //viewportSectionsIn.setView(jListSectionsIn);

        jListGroupsIn.addListSelectionListener(this);
        jListGroupsOut.addListSelectionListener(this);

    }

    private void extraInit()
    {
        jComboBoxProcessNames.addItem(defaultProcessSelection);


        for (int i = 0; i < processOptions.size(); i++)
        {
            jComboBoxProcessNames.addItem(processOptions.elementAt(i));
        }

        logger.logComment("Finished initialising...");
    }

    public void valueChanged(ListSelectionEvent e)
    {
        logger.logComment("Value changed: " + e);

        if (e.getSource().equals(jListGroupsIn))
        {
            logger.logComment("GroupsIn change: " + e.getFirstIndex());

            this.jButtonRemove.setEnabled(true);
            this.jButtonAdd.setEnabled(false);

            jListGroupsOut.setSelectedIndices(new int[]
                                                {});

            if (e.getValueIsAdjusting())
            {
                logger.logComment("Selected: " + e.getFirstIndex());
            }
        }
        else if (e.getSource().equals(jListGroupsOut))
        {
            logger.logComment("SectionsOut change: " + e.getFirstIndex());
            this.jButtonRemove.setEnabled(false);
            this.jButtonAdd.setEnabled(true);

            jListGroupsIn.setSelectedIndices(new int[]
                                               {});

            if (e.getValueIsAdjusting())
            {
                logger.logComment("Selected: " + e.getFirstIndex());
            }
        }

    }

    void jButtonCancel_actionPerformed(ActionEvent e)
    {
        logger.logComment("Cancel button pressed");
        cancelled = true;
        this.dispose();
    }

    void jButtonOK_actionPerformed(ActionEvent e)
    {
        logger.logComment("OK button pressed");

        this.dispose();
    }

    public static void main(String[] args)
    {
        String favouredLookAndFeel = MainApplication.getFavouredLookAndFeel();
        try
        {
            UIManager.setLookAndFeel(favouredLookAndFeel);
        }
        catch (Exception ex)
        {

        }
        SimpleCell cell = new SimpleCell("");

        cell.associateGroupWithSynapse("all", "syno");

        Vector list = new Vector();
        list.add("gg");
        list.add("hh");
        list.add("jj");


        EditGroupSynapseAssociations dlg = new EditGroupSynapseAssociations(cell, null, "Synapse", list);

        dlg.setModal(true);
        dlg.setVisible(true);

    }

    void jComboBoxGroupNames_itemStateChanged(ItemEvent e)
    {
        logger.logComment("" + e);

        if (e.getStateChange() == ItemEvent.SELECTED)
        {
            listModelGroupsIn.clear();
            listModelGroupsOut.clear();


            String selectedProcess = (String) jComboBoxProcessNames.getSelectedItem();

            logger.logComment("Setting the selected process to: " + selectedProcess);

            if (selectedProcess.equals(defaultProcessSelection))
            {
                return;
            }

            Vector allGroups = myCell.getAllGroupNames();

            Vector appropriateProcesses = myCell.getGroupsWithSynapse(selectedProcess);


            for (int i = 0; i < allGroups.size(); i++)
            {
                  String nextGroup = (String)allGroups.elementAt(i);
                  if (appropriateProcesses.contains(nextGroup))
                  {
                      listModelGroupsIn.addElement(nextGroup);
                  }
                  else
                  {
                      listModelGroupsOut.addElement(nextGroup);
                  }
            }

        }
    }

    void jButtonAdd_actionPerformed(ActionEvent e)
    {
        String selectedProcess = (String) jComboBoxProcessNames.getSelectedItem();

        if (selectedProcess.equals(defaultProcessSelection)) return; // Why???

        int[] selected = jListGroupsOut.getSelectedIndices();

        for (int i = 0; i < selected.length; i++)
        {
            String group = (String)listModelGroupsOut.elementAt(selected[i]);
            logger.logComment("Item: " + selected[i] + " ("+group+") is selected...");

            myCell.associateGroupWithSynapse(group, selectedProcess);

        }

        // simple update...
        this.jComboBoxProcessNames.setSelectedItem(defaultProcessSelection);
        this.jComboBoxProcessNames.setSelectedItem(selectedProcess);
    }


    void jButtonRemove_actionPerformed(ActionEvent e)
    {
        String selectedProcess = (String) jComboBoxProcessNames.getSelectedItem();

        if (selectedProcess.equals(defaultProcessSelection)) return; // Why???

        int[] selected = jListGroupsIn.getSelectedIndices();

        for (int i = 0; i < selected.length; i++)
        {
            String group = (String)listModelGroupsIn.elementAt(selected[i]);
            logger.logComment("Item: " + selected[i] + " ("+group+") is selected...");

            myCell.disassociateGroupFromSynapse(group, selectedProcess);

        }

        // simple update...
        this.jComboBoxProcessNames.setSelectedItem(defaultProcessSelection);
        this.jComboBoxProcessNames.setSelectedItem(selectedProcess);

    }



}
