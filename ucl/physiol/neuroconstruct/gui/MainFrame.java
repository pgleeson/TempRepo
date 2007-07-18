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

import java.beans.*;
import java.io.*;
import java.text.*;
import java.util.*;
import java.util.zip.*;
import javax.media.j3d.*;
import javax.vecmath.*;
import javax.xml.*;
import javax.xml.parsers.*;
import javax.xml.transform.*;
import javax.xml.transform.stream.*;
import javax.xml.validation.*;

import java.awt.*;
import java.awt.event.*;
import javax.swing.*;
import javax.swing.border.*;
import javax.swing.event.*;
import javax.swing.table.*;

import org.xml.sax.*;
import ucl.physiol.neuroconstruct.cell.*;
import ucl.physiol.neuroconstruct.cell.compartmentalisation.*;
import ucl.physiol.neuroconstruct.cell.converters.*;
import ucl.physiol.neuroconstruct.cell.examples.*;
import ucl.physiol.neuroconstruct.cell.utils.*;
import ucl.physiol.neuroconstruct.dataset.*;
import ucl.physiol.neuroconstruct.genesis.*;
import ucl.physiol.neuroconstruct.gui.plotter.*;
import ucl.physiol.neuroconstruct.hpc.condor.*;
import ucl.physiol.neuroconstruct.hpc.mpi.*;
import ucl.physiol.neuroconstruct.j3D.*;
import ucl.physiol.neuroconstruct.mechanisms.*;
import ucl.physiol.neuroconstruct.neuroml.*;
import ucl.physiol.neuroconstruct.neuron.*;
import ucl.physiol.neuroconstruct.nmodleditor.gui.*;
import ucl.physiol.neuroconstruct.nmodleditor.processes.*;
import ucl.physiol.neuroconstruct.project.*;
import ucl.physiol.neuroconstruct.project.GeneratedNetworkConnections.*;
import ucl.physiol.neuroconstruct.project.cellchoice.*;
import ucl.physiol.neuroconstruct.project.packing.*;
import ucl.physiol.neuroconstruct.simulation.*;
import ucl.physiol.neuroconstruct.utils.*;
import ucl.physiol.neuroconstruct.utils.units.*;
import ucl.physiol.neuroconstruct.utils.xml.*;

/**
 * The big class. The main neuroConstruct frame, lots of GUI stuff. A lot of the
 * non gui specific stuff should be moved to ProjectManager
 *
 * @author Padraig Gleeson
 * @version 1.0.3
 */

@SuppressWarnings("serial")
public class MainFrame extends JFrame implements ProjectEventListener, GenerationReport
{
    ClassLogger logger = new ClassLogger("MainFrame");

    public ProjectManager projManager = new ProjectManager(this, this);

    Base3DPanel base3DPanel = null;

    boolean initialisingProject = false;

    ToolTipHelper toolTipText = ToolTipHelper.getInstance();

    RecentFiles recentFiles = RecentFiles.getRecentFilesInstance(ProjectStructure.getNeuConRecentFilesFilename());

    String PROJECT_INFO_TAB = "Project";
    String CELL_TYPES_TAB = "Cell Types";
    String REGIONS_TAB = "Regions";
    String CELL_GROUPS_TAB = "Cell Groups";
    String CELL_MECHANISM_TAB = "Cell Mechanisms";
    String NETWORK_TAB = "Network";
    String GENERATE_TAB = "Generate";
    String VISUALISATION_TAB = "Visualisation";
    String EXPORT_TAB = "Export Network";

    String SIMULATOR_SETTINGS_TAB = "Common Simulation Settings";
    String INPUT_OUTPUT_TAB = "Input and Output";

    String NEURON_SIMULATOR_TAB = "NEURON";
    String NEURON_TAB_GENERATE = "Generate code";
    String NEURON_TAB_EXTRA = "Extra hoc code";


    String GENESIS_SIMULATOR_TAB = "GENESIS";
    String GENESIS_TAB_GENERATE = "Generate code";
    String GENESIS_TAB_EXTRA = "Extra GENESIS code";

    String MORPHML_TAB = "NeuroML";

    OptionsFrame optFrame = null;

    // needed for changing panel at stim settings...
    // IClamp stuff:
    JTextField jTextFieldIClampAmplitude = new JTextField(12);
    JTextField jTextFieldIClampDuration = new JTextField(12);
    // NetStim stuff:
    JTextField jTextFieldNetStimNumber = new JTextField(12);
    JTextField jTextFieldNetStimNoise = new JTextField(12);

    String defaultAnalyseCellGroupString = "-- Please select a Cell Group --";
    String defaultAnalyseNetConnString = "-- Please select a Network Connection --";


    // For figuring out whether the positions were generated anew, or were from prev simulations
    private final static int NO_POSITIONS_LOADED = 0;
    private final static int GENERATED_POSITIONS = 1;
    //private final static int RECORDED_POSITIONS = 2;
    //private final static int STORED_POSITIONS = 3;
    private final static int RELOADED_POSITIONS = 19;
    private final static int NETWORKML_POSITIONS = 21;

    int sourceOfCellPosnsInMemory = NO_POSITIONS_LOADED;

    //String currentlyLoadedSimRef = null;


    public static final String LATEST_GENERATED_POSITIONS = "Latest Generated Positions";

    String choice3DChoiceMain  = "     -- Please select: --";
    String choice3DSingleCells = "     -- Cell Types: --";
    String choice3DPrevSims    = "     -- NEURON Simulations: --";


    String defaultCellTypeToView = "-- Select a Cell Type --";

    String noNeuroMLFilesFound = "-- No NeuroML files found --";

    String defaultNeuronFilesText = "-- Select NEURON file to view --";
    String defaultGenesisFilesText = "-- Select GENESIS file to view --";

    private int neuronRunMode = NeuronFileManager.RUN_LOCALLY;


    private String welcomeText =
        "\nNo neuroConstruct project loaded.\n\n"+
        "To create a new project select: File -> New Project... in the main menu.\n\n"
        +"To open an existing project select: File -> Open Project... or choose one of the projects listed at the bottom of that menu.\n\n"+
        "For tutorials on neuroConstruct select menu: Help -> Help and follow the link for the tutorials.\n\n";

    // JBuilder added stuff...

    JPanel contentPane;
    JMenuBar jMenuBar1 = new JMenuBar();
    JMenu jMenuFile = new JMenu();
    JMenuItem jMenuFileExit = new JMenuItem();
    JMenu jMenuHelp = new JMenu();
    JMenuItem jMenuHelpAbout = new JMenuItem();
    JToolBar jToolBar = new JToolBar();
    JButton jButtonOpenProject = new JButton();
    JButton jButtonSaveProject = new JButton();
    JButton jButtonPreferences = new JButton();
    JButton jButtonCloseProject = new JButton();
    JButton jButtonToggleTips = new JButton();
    JButton jButtonToggleConsoleOut = new JButton();

    ImageIcon imageNewProject;
    ImageIcon imageOpenProject;
    ImageIcon imageSaveProject;
    ImageIcon imageCloseProject;
    ImageIcon imageProjectPrefs;
    ImageIcon imageTips;
    ImageIcon imageNoTips;
    ImageIcon imageConsoleOut;
    ImageIcon imageNoConsoleOut;

    JLabel statusBar = new JLabel();
    BorderLayout borderLayout1 = new BorderLayout();
    JMenuItem jMenuItemFileOpen = new JMenuItem();
    JMenuItem jMenuItemSaveProject = new JMenuItem();
    JMenu jMenuSettings = new JMenu();
    JMenuItem jMenuItemProjProperties = new JMenuItem();
    JMenu jMenuTools = new JMenu();
    JMenuItem jMenuItemNmodlEditor = new JMenuItem();
    JMenuItem jMenuItemNewProject = new JMenuItem();
    JButton jButtonNewProject = new JButton();
    JButton jButtonValidate = new JButton();
    JTabbedPane jTabbedPaneMain = new JTabbedPane();
    JTabbedPane jTabbedPaneExportFormats = new JTabbedPane();

    JPanel jPanelExportNeuron = new JPanel();
    JPanel jPanelExportGenesis = new JPanel();
    //JPanel jPanelExportNeosim = new JPanel();


    JPanel jPanelProjInfo = new JPanel();
    JPanel jPanelRegions = new JPanel();
    JPanel jPanelCellTypes = new JPanel();
    JPanel jPanelCellGroupDetails = new JPanel();
    JPanel jPanel3DDemo = new JPanel();
    BorderLayout borderLayout2 = new BorderLayout();
    BorderLayout borderLayout3 = new BorderLayout();
    JButton jButtonCellTypeNew = new JButton();
    JPanel jPanelRegionsButtons = new JPanel();
    JButton jButtonRegionNew = new JButton();
    JPanel jPanelRegionsTable = new JPanel();
    BorderLayout borderLayout4 = new BorderLayout();
    JPanel jPanel3DMain = new JPanel();
    JPanel jPanel3DButtons = new JPanel();
    BorderLayout borderLayout5 = new BorderLayout();
    JButton jButton3DView = new JButton();
   // JPanel jPanelCellTypeAddNew = new JPanel();
    JPanel jPanelCellTypeDetails = new JPanel();
    JTree jTreeCellDetails = null;
    JPanel jPanelHocFileButtons = new JPanel();
    JPanel jPanelCellGroupsMainPanel = new JPanel();
    BorderLayout borderLayout6 = new BorderLayout();
    JButton jButtonNeuronRun = new JButton();
    JButton jButtonNeuronCreateLocal = new JButton();
    JButton jButton3DDestroy = new JButton();
    //JLabel jLabelWidth = new JLabel();
    //JTextField jTextFieldWidth = new JTextField();
    //JLabel jLabelDepth = new JLabel();
    //JTextField jTextFieldDepth = new JTextField();
    JScrollPane jScrollPaneRegions = new JScrollPane();

    JTable jTable3DRegions = new JTable();

    JPanel jPanelCellGroupButtons = new JPanel();
    JButton jButtonCellGroupsNew = new JButton();
    JScrollPane jScrollPaneCellGroups = new JScrollPane();
    JTable jTableCellGroups = new JTable();
    BorderLayout borderLayout7 = new BorderLayout();
    BorderLayout borderLayout8 = new BorderLayout();
    JPanel jPanelMainInfo = new JPanel();
    JLabel jLabelName = new JLabel();
    JLabel jLabelMainNumCells = new JLabel();
    JLabel jLabelProjDescription = new JLabel();
    JTextField jTextFieldProjName = new JTextField();
    GridBagLayout gridBagLayout2 = new GridBagLayout();
    JTextArea jTextAreaProjDescription = new JTextArea();

    JScrollPane jScrollPaneProjDesc = new JScrollPane();

    ClickPanel jPanelSimConfigClicks = new ClickPanel();

    JLabel jLabelSimConfigs = new JLabel();
    //JTextField jTextFieldNumCells = new JTextField();
    ClickPanel jPanelCellClicks = new ClickPanel();


   // JComboBox jComboBox3DCellToView = new JComboBox();
    JPanel jPanelNetworkSettings = new JPanel();
    JMenuItem jMenuItemCloseProject = new JMenuItem();
    JLabel jLabelTitle = new JLabel();
    JPanel jPanelExport = new JPanel();
    BorderLayout borderLayout10 = new BorderLayout();
    JLabel jLabelSimDefDur = new JLabel();
    JTextField jTextFieldSimDefDur = new JTextField();
    JPanel jPanelNetSetSimple = new JPanel();

    String defaultCellGroupStimulation = new String("-- No Stimulation --");
    String cellComboPrompt = new String("-- Please select a cell type --");

    String neuronBlockPrompt = new String("-- Please select where to put NEURON code --");
    String genesisBlockPrompt = new String("-- Please select where to put GENESIS script --");

    JPanel jPanelSimSettings = new JPanel();
    JLabel jLabelSimDT = new JLabel();
    JTextField jTextFieldSimDT = new JTextField();

    JPanel jPanelSimValsInput = new JPanel();
    BorderLayout borderLayout12 = new BorderLayout();
    JLabel jLabelSimSummary = new JLabel();
    JPanel jPanelCellTypeInfo = new JPanel();
    JButton jButton3DSettings = new JButton();
    JButton jButtonNeuronView = new JButton();
    JPanel jPanelSimStorage = new JPanel();
    JLabel jLabelSimRef = new JLabel();
    JTextField jTextFieldSimRef = new JTextField();
    JRadioButton jRadioButtonNeuronSimSaveToFile = new JRadioButton();
    ButtonGroup buttonGroupSimSavePreference = new ButtonGroup();
    JPanel jPanelNetSetControls = new JPanel();
    JButton jButtonNetSetAddNew = new JButton();
    JPanel jPanelNetSetTable = new JPanel();
    BorderLayout borderLayout13 = new BorderLayout();
    JButton jButtonCellTypeViewCell = new JButton();
    JButton jButtonCellTypeViewCellChans = new JButton();
    JLabel jLabelExistingCellTypes = new JLabel();
    JComboBox jComboBoxCellTypes = new JComboBox();

    JComboBox jComboBoxNeuronExtraBlocks = new JComboBox();
    JPanel jPanelCBNeuronExtraBlocks = new JPanel();

    JComboBox jComboBoxGenesisExtraBlocks = new JComboBox();
    JPanel jPanelCBGenesisExtraBlocks = new JPanel();

    JScrollPane scrollerCellTypeInfo = new JScrollPane();
    JEditorPane jEditorPaneCellTypeInfo = new JEditorPane();
    JEditorPane jEditorPaneGenerateInfo = new JEditorPane();
    JPanel jPanelGenerate = new JPanel();
    JPanel jPanelGenerateMain = new JPanel();
    JPanel jPanelGenerateButtonsDesc = new JPanel();
    JPanel jPanelGenerateLoadSave = new JPanel();
    JPanel jPanelGenerateButtons = new JPanel();
    JPanel jPanelGenerateDesc = new JPanel();
    BorderLayout borderLayout14 = new BorderLayout();
    JButton jButtonGenerate = new JButton();
    JComboBox jComboBoxSimConfig = new JComboBox();

    JButton jButtonGenerateSave = new JButton();
    JButton jButtonGenerateLoad = new JButton();
    JCheckBox jCheckBoxGenerateZip = new JCheckBox();
    JCheckBox jCheckBoxGenerateExtraNetComments = new JCheckBox();


    JButton jButtonSimConfigEdit = new JButton();
    JScrollPane scrollerGenerate = new JScrollPane();
    JButton jButtonRegionRemove = new JButton();
    JButton jButtonCellGroupsDelete = new JButton();
    JScrollPane jScrollPaneNetConnects = new JScrollPane();
    JScrollPane jScrollPaneAAConns = new JScrollPane();
    JTable jTableNetConns = new JTable();
    JTable jTableAAConns = new JTable();


    BorderLayout borderLayout15 = new BorderLayout();
    JButton jButtonNetConnDelete = new JButton();
    JRadioButton jRadioButtonNeuronSimDontRecord = new JRadioButton();
    ImageIcon imageNeuroConstruct = null;
    Border border1;
    GridBagLayout gridBagLayout4 = new GridBagLayout();
    JPanel jPanelNmodl = new JPanel();
    JPanel jPanelSynapseButtons = new JPanel();
    JPanel jPanelSynapseMain = new JPanel();
    ///JButton jButtonSynapseAdd = new JButton();
    JScrollPane jScrollPaneSynapses = new JScrollPane();
    JScrollPane jScrollPaneChanMechs = new JScrollPane();

    JTable jTableSynapses = new JTable();
    JTable jTableChanMechs = new JTable();

    BorderLayout borderLayout16 = new BorderLayout();
    JPanel jPanelStims = new JPanel();
    BorderLayout borderLayout17 = new BorderLayout();
    ButtonGroup buttonGroupStim = new ButtonGroup();
    ButtonGroup buttonGroupStimCellChoice = new ButtonGroup();
    JPanel jPanelGenerateAnalyse = new JPanel();
    JLabel jLabelGenAnalyse = new JLabel();
    JComboBox jComboBoxAnalyseCellGroup = new JComboBox();
    JComboBox jComboBoxAnalyseNetConn = new JComboBox();
    GridBagLayout gridBagLayout1 = new GridBagLayout();
    JComboBox jComboBoxView3DChoice = new JComboBox();
    JButton jButtonSynapseEdit = new JButton();
    JMenuItem jMenuItemGeneralProps = new JMenuItem();
    JButton jButtonGenerateStop = new JButton();
    JTextArea jTextAreaSimConfigDesc = new JTextArea();


    JPanel jPanelRandomGen =  new JPanel();
    JLabel jLabelRandomGenDesc = new JLabel("Random seed for generation:");
    JTextField jTextFieldRandomGen = new JTextField();
    JCheckBox jCheckBoxRandomGen = new JCheckBox("Recalculate before network generation");

    JPanel jPanelNeuronNumInt =  new JPanel();
    JCheckBox jCheckBoxNeuronNumInt = new JCheckBox("Use variable time step");

    JPanel jPanelNeuronRandomGen =  new JPanel();
    JLabel jLabelNeuronRandomGenDesc = new JLabel("Random seed for NEURON:");
    JTextField jTextFieldNeuronRandomGen = new JTextField();
    JCheckBox jCheckBoxNeuronRandomGen = new JCheckBox("Recalculate before creating hoc files");

    JPanel jPanelGenesisRandomGen =  new JPanel();
    JPanel jPanelGenesisComps =  new JPanel();
    JLabel jLabelGenesisCompsDesc = new JLabel("Compartmentalisation to use:");

    JLabel jLabelGenesisRandomGenDesc = new JLabel("Random seed for GENESIS:");
    JTextField jTextFieldGenesisRandomGen = new JTextField();
    JCheckBox jCheckBoxGenesisRandomGen = new JCheckBox("Recalculate before creating GENESIS scripts");


    JButton jButtonNetConnEdit = new JButton();
    JButton jButtonAnalyseConnLengths = new JButton();
    JButton jButtonAnalyseNumConns = new JButton();

    JButton jButtonAnalyseCellDensities = new JButton();

    JPanel jPanelGenerateComboBoxes = new JPanel();
    JPanel jPanelGenerateAnalyseButtons = new JPanel();
    BorderLayout borderLayout18 = new BorderLayout();
    //JPanel jPanelSynapticProcesses = new JPanel();
    BorderLayout borderLayout19 = new BorderLayout();
    JPanel jPanelChannelMechsInnerTab = new JPanel();
    JPanel jPanelChannelMechsMain = new JPanel();
    //JPanel jPanelChanMechsButtons = new JPanel();
    //JButton jButtonChanMechAdd = new JButton();
    ///JButton jButtonChanMechEdit = new JButton();
    BorderLayout borderLayout20 = new BorderLayout();
    GridLayout gridLayout1 = new GridLayout();
    BorderLayout borderLayout11 = new BorderLayout();
    JLabel jLabelChanMechTitle = new JLabel();
    Border border2;
    JLabel jLabelSynapseTitle = new JLabel();
    JMenuItem jMenuItemZipUp = new JMenuItem();
    JLabel jLabelNumCellGroups = new JLabel();


    //JTextField jTextFieldNumCellGroups = new JTextField();
    ClickPanel jPanelCellGroupClicks = new ClickPanel();

    JLabel jLabelProjFileVersion = new JLabel();
    JTextField jTextFieldProjFileVersion = new JTextField();
    JMenuItem jMenuItemUnzipProject = new JMenuItem();
    JPanel jPanelAllNetSettings = new JPanel();
    JPanel jPanelNetSetAA = new JPanel();
    BorderLayout borderLayout9 = new BorderLayout();
    GridLayout gridLayout2 = new GridLayout();
    GridLayout gridLayout3 = new GridLayout();
    JPanel jPanelSynapseButtonsOnly = new JPanel();
    BorderLayout borderLayout21 = new BorderLayout();
    JPanel jPanelChanMechsButtonsOnly = new JPanel();
    BorderLayout borderLayout22 = new BorderLayout();
    JPanel jPanelNetConnButtonsOnly = new JPanel();
    JLabel jLabelNetConnSimpleConn = new JLabel();
    BorderLayout borderLayout23 = new BorderLayout();
    JPanel jPanelNetSetAAControls = new JPanel();
    JPanel jPanelNetSetAATable = new JPanel();
    JLabel jLabelNetSetAA = new JLabel();
    JButton jButtonNetAAAdd = new JButton();
    JPanel jPanelNetSetAAConButtons = new JPanel();
    BorderLayout borderLayout24 = new BorderLayout();
    BorderLayout borderLayout25 = new BorderLayout();
    JButton jButtonCellGroupsEdit = new JButton();
    GridLayout gridLayout4 = new GridLayout();
    JButton jButtonNetAADelete = new JButton();
    JButton jButtonNetAAEdit = new JButton();
    JMenuItem jMenuItemViewProjSource = new JMenuItem();
    JPanel jPanelCellTypeMainInfo = new JPanel();
    JButton jButtonCellTypeViewCellInfo = new JButton();
    JPanel jPanelGenesisMain = new JPanel();
    BorderLayout borderLayout26 = new BorderLayout();
    JLabel jLabelGenesisMain = new JLabel();
    JPanel jPanelSimNeosimMain = new JPanel();
    JLabel jLabelSimulatorNeosimMain = new JLabel();
    BorderLayout borderLayout27 = new BorderLayout();
    JButton jButtonCellTypeDelete = new JButton();
    JButton jButtonCellTypeCompare = new JButton();
    JPanel jPanelCellTypeMainButtons = new JPanel();
    //JPanel jPanelCellTypesAddNew = new JPanel();
    BorderLayout borderLayout29 = new BorderLayout();
    BorderLayout borderLayout30 = new BorderLayout();
    JPanel jPanelCellTypesAddNewCell = new JPanel();
    JPanel jPanelCellTypesComboBox = new JPanel();
    FlowLayout flowLayout1 = new FlowLayout();
    JButton jButtonCellTypeCopy = new JButton();
    JPanel jPanelCellTypesButtonsInfo = new JPanel();
    JButton jButtonCellTypesMoveToOrigin = new JButton();
    JPanel jPanelCellTypeManageNumbers = new JPanel();
    JPanel jPanelCellTypesModify = new JPanel();
    JButton jButtonCellTypesConnect = new JButton();
    JButton jButtonCellTypesMakeSimpConn = new JButton();
    FlowLayout flowLayout3 = new FlowLayout();
    JLabel jLabelMainLastModified = new JLabel();
    JTextField jTextFieldMainLastModified = new JTextField();
    JMenuItem jMenuItemGlossary = new JMenuItem();
    JPanel jPanelExportHeader = new JPanel();
    JLabel jLabelExportMain = new JLabel();
    //JPanel jPanelExportMorphML = new JPanel();
    JTabbedPane jTabbedPaneNeuron = new JTabbedPane();
    JPanel jPanelNeuronMainSettings = new JPanel();
    GridLayout gridLayout5 = new GridLayout();
    JPanel jPanelNeuroML = new JPanel();

    JButton jButtonNeuroMLExportLevel1 = new JButton();
    JButton jButtonNeuroMLExportLevel2 = new JButton();
    JButton jButtonNeuroMLExportCellLevel3 = new JButton();
    //JButton jButtonNeuroMLExportNetLevel3 = new JButton();

    JLabel jLabelNeuroMLMain = new JLabel();
    JPanel jPanelNeuroMLButtons = new JPanel();
    JPanel jPanelNeuroMLHeader = new JPanel();


    JComboBox jComboBoxNeuroMLComps = new JComboBox();
    JTextArea jTextAreaNeuroMLCompsDesc = new JTextArea();


    JComboBox jComboBoxGenesisComps = new JComboBox();
    JTextArea jTextAreaGenesisCompsDesc = new JTextArea();



    BorderLayout borderLayout31 = new BorderLayout();
    GridLayout gridLayout6 = new GridLayout();
    JPanel jPanelNeuroMLView = new JPanel();
    JLabel jLabelNeuroMLGeneratedFiles = new JLabel();
    JComboBox jComboBoxNeuroML = new JComboBox();
    JButton jButtonNeuroMLViewPlain = new JButton();
    JButton jButtonNeuroMLViewFormatted = new JButton();
    GridBagLayout gridBagLayout3 = new GridBagLayout();
    JPanel jPanelGenesisButtons = new JPanel();
    JPanel jPanelGenesisSettings = new JPanel();
    JButton jButtonGenesisGenerate = new JButton();
    JButton jButtonGenesisRun = new JButton();
    BorderLayout borderLayout32 = new BorderLayout();
    JButton jButtonGenesisView = new JButton();
    JButton jButtonRegionsEdit = new JButton();
    JPanel jPanelNeuronExtraHoc = new JPanel();
    JPanel jPanelNeuronExtraHocBlock = new JPanel();


    //JPanel jPanelNeuronExtraHocAfter = new JPanel();
    GridLayout gridLayout7 = new GridLayout();

    JPanel jPanelNeuronBlockDesc = new JPanel();
    JPanel jPanelGenesisBlockDesc = new JPanel();

    JTextArea jTextAreaNeuronBlockDesc = new JTextArea();
    JTextArea jTextAreaGenesisBlockDesc = new JTextArea();


    JScrollPane jScrollPaneNeuronBlock = new JScrollPane();
    JScrollPane jScrollPaneGenesisBlock = new JScrollPane();


    Border border4;
    JTextArea jTextAreaNeuronBlock = new JTextArea();
    JTextArea jTextAreaGenesisBlock = new JTextArea();
    ////JTextArea jTextAreaNeuronAfter = new JTextArea();
    FlowLayout flowLayout4 = new FlowLayout();
    FlowLayout flowLayout5 = new FlowLayout();
    JComboBox jComboBoxNeuronFiles = new JComboBox();
    Border border5;
    Border border6;
    JMenuItem jMenuItemCondorMonitor = new JMenuItem();


    JMenuItem jMenuItemPlotEquation = new JMenuItem();
    JMenuItem jMenuItemPlotImport = new JMenuItem();

    JMenuItem jMenuItemMPIMonitor = new JMenuItem();
    JCheckBox jCheckBoxSpecifySimRef = new JCheckBox();
    JCheckBox jCheckBoxNeuronSaveHoc = new JCheckBox();
    JButton jButtonNeuronCreateCondor = new JButton();
    JButton jButtonNeuronCreateMPI = new JButton();
    JComboBox jComboBoxGenesisFiles = new JComboBox();
    JPanel jPanelSimGeneral = new JPanel();
    JPanel jPanelInputOutput = new JPanel();
    BorderLayout borderLayout33 = new BorderLayout();
    JLabel jLabelNeuronMainLabel = new JLabel();
    Border border7;
    JPanel jPanelSimulationParams = new JPanel();
    JLabel jLabelSimulationGlobRa = new JLabel();
    JTextField jTextFieldSimulationGlobRa = new JTextField();
    JLabel jLabelSimulationGlobCm = new JLabel();
    JTextField jTextFieldSimulationGlobCm = new JTextField();
    JTabbedPane jTabbedPaneGenesis = new JTabbedPane();
    //JPanel jPanelGenesisExtraBefore = new JPanel();
    //JLabel jLabelGenesisExtraBefore = new JLabel();
    FlowLayout flowLayout6 = new FlowLayout();
    //JTextArea jTextAreaGenesisAfter = new JTextArea();
    //JPanel jPanelGenesisExtraAfter = new JPanel();
    JPanel jPanelGenesisExtra = new JPanel();

    JPanel jPanelGenesisExtraBlock = new JPanel();
    FlowLayout flowLayout7 = new FlowLayout();
    GridLayout gridLayout8 = new GridLayout();
    //JScrollPane jScrollPaneGenesisAfter = new JScrollPane();
    //JLabel jLabelGenesisExtraAfter = new JLabel();
    //JScrollPane jScrollPaneGenesisBefore = new JScrollPane();
    //JTextArea jTextAreaGenesisBefore = new JTextArea();
    JPanel jPanelSimulationGlobal = new JPanel();
    JButton jButton3DPrevSims = new JButton();
    JLabel jLabelSimulationGlobRm = new JLabel();
    JTextField jTextFieldSimulationGlobRm = new JTextField();
    JLabel jLabelSimulationInitVm = new JLabel();
    JTextField jTextFieldSimulationInitVm = new JTextField();
    JLabel jLabelSimulationVLeak = new JLabel();
    JTextField jTextFieldSimulationVLeak = new JTextField();
    JCheckBox jCheckBoxGenesisSymmetric = new JCheckBox();
    BorderLayout borderLayout35 = new BorderLayout();
    Border border8;
    JPanel jPanelNeuronGraphOptions = new JPanel();


    GridBagLayout gridBagLayout6 = new GridBagLayout();
    GridBagLayout gridBagLayoutGen = new GridBagLayout();


    JCheckBox jCheckBoxNeuronShowShapePlot = new JCheckBox();

    JCheckBox jCheckBoxNeuronNoGraphicsMode = new JCheckBox();
    JCheckBox jCheckBoxGenesisNoGraphicsMode = new JCheckBox();


    JPanel jPanelSimWhatToRecord = new JPanel();
    JRadioButton jRadioButtonSimSomaOnly = new JRadioButton();
    JLabel jLabelSimWhatToRecord = new JLabel();
    JRadioButton jRadioButtonSimAllSegments = new JRadioButton();
    ButtonGroup buttonGroupSimWhatToRecord = new ButtonGroup();
    JRadioButton jRadioButtonSimAllSections = new JRadioButton();
    ButtonGroup buttonGroupGenesisUnits = new ButtonGroup();
    JPanel jPanelGenesisUnits = new JPanel();
    JRadioButton jRadioButtonGenesisPhy = new JRadioButton();
    JRadioButton jRadioButtonGenesisSI = new JRadioButton();
    JPanel jPanelCellMechanisms = new JPanel();
    JPanel jPanelProcessButtons = new JPanel();
    JPanel jPanelProcessButtonsTop = new JPanel();
    JPanel jPanelProcessButtonsBottom = new JPanel();
    JPanel jPanelMechanismMain = new JPanel();
    JPanel jPanelMechanismLabel = new JPanel();
    BorderLayout borderLayout28 = new BorderLayout();
    JLabel JLabelMechanismMain = new JLabel();
    JScrollPane jScrollPaneMechanisms = new JScrollPane();
    BorderLayout borderLayout36 = new BorderLayout();
    JTable jTableMechanisms = new JTable();
    JButton jButtonMechanismDelete = new JButton();
    JButton jButtonMechanismEdit = new JButton();
    JButton jButtonMechanismAbstract = new JButton();
    JButton jButtonMechanismTemplateCML = new JButton();
    BorderLayout borderLayout37 = new BorderLayout();
    Border border9;
    JMenuItem jMenuItemUnits = new JMenuItem();
    JPanel jPanelGenesisChoices = new JPanel();
    BorderLayout borderLayout38 = new BorderLayout();
    JButton jButtonMechanismFileBased = new JButton();
    JButton jButtonMechanismNewCML = new JButton();
    JLabel jLabelSimulationTemp = new JLabel();
    JTextField jTextFieldSimulationTemperature = new JTextField();
    JCheckBox jCheckBoxGenesisComments = new JCheckBox();
    JCheckBox jCheckBoxGenesisShapePlot = new JCheckBox();

    JLabel jLabelSimStimDesc = new JLabel();

    JPanel jPanelGenesisCheckBoxes = new JPanel();
    JProgressBar jProgressBarGenerate = new JProgressBar();
    JPanel jPanelGenesisNumMethod = new JPanel();
    JLabel jLabelGenesisNumMethod = new JLabel();
    JButton jButtonGenesisNumMethod = new JButton();
    JMenuItem jMenuItemCopyProject = new JMenuItem();
    JPanel jPanelSimPlot = new JPanel();
    BorderLayout borderLayout39 = new BorderLayout();
    JPanel jPanelSimRecordWhere = new JPanel();
    BorderLayout borderLayout40 = new BorderLayout();
    JPanel jPanelSimTotalTime = new JPanel();
    BorderLayout borderLayout41 = new BorderLayout();
    Border border10;
    JPanel jPanelSimDT = new JPanel();
    //JTextField jTextFieldNeuronDuration1 = new JTextField();
    //JLabel jLabelNeuronDuration1 = new JLabel();

    JTextField jTextFieldSimTotalTimeUnits = new JTextField();
    JTextField jTextFieldSimDTUnits = new JTextField();
    JScrollPane jScrollPaneSimPlot = new JScrollPane();
    BorderLayout borderLayout42 = new BorderLayout();
    JPanel jPanelSimPlotButtons = new JPanel();
    JButton jButtonSimPlotAdd = new JButton();
    JTable jTableSimPlot = new JTable();
    JTable jTableStims = new JTable();

    JButton jButtonSimPlotDelete = new JButton();
    JButton jButtonSimPlotEdit = new JButton();
    JPanel jPanelSimStimButtons = new JPanel();
    JScrollPane jScrollPaneSimStims = new JScrollPane();
    JButton jButtonSimStimAdd = new JButton();
    JButton jButtonSimStimDelete = new JButton();
    JButton jButtonSimStimEdit = new JButton();
    GridBagLayout gridBagLayout5 = new GridBagLayout();
    JTextField jTextFieldSimUnitInitVm = new JTextField();
    JTextField jTextFieldSimUnitVLeak = new JTextField();
    BorderLayout borderLayout34 = new BorderLayout();
    JTextField jTextFieldSimUnitGlobRa = new JTextField();
    JTextField jTextFieldSimUnitGlotCm = new JTextField();
    JTextField jTextFieldSimUnitGlobRm = new JTextField();
    JTextField jTextFieldSimUnitTemp = new JTextField();
    JMenuItem jMenuItemHelp = new JMenuItem();
    JCheckBox jCheckBoxNeuronComments = new JCheckBox();
    JButton jButtonCellTypeBioPhys = new JButton();
    JButton jButtonCellTypeEditDesc = new JButton();

    JButton jButtonCellTypeOtherProject = new JButton();
    JMenuItem jMenuItemJava = new JMenuItem();
    JMenu jMenuProject = new JMenu();
    JMenuItem jMenuItemGenNetwork = new JMenuItem();
    JMenuItem jMenuItemGenNeuron = new JMenuItem();
    JMenuItem jMenuItemGenGenesis = new JMenuItem();
    JMenuItem jMenuItemPrevSims = new JMenuItem();
    JMenuItem jMenuItemDataSets = new JMenuItem();
    JMenuItem jMenuItemListSims = new JMenuItem();


   // JButton jButtonSimulationRecord = new JButton();

    public MainFrame()
    {
        enableEvents(AWTEvent.WINDOW_EVENT_MASK);
        try
        {
            jbInit();
            extraInit();

            addToolTips();

            refreshAll();
        }
        catch (Exception e)
        {
            logger.logError("Exception starting GUI: ", e);
        }
    }

    private void jbInit() throws Exception
    {
        border2 = BorderFactory.createEmptyBorder(5,0,5,0);

        border4 = BorderFactory.createEmptyBorder(8,8,8,8);
        border5 = BorderFactory.createEmptyBorder(5,5,5,5);
        border6 = BorderFactory.createEmptyBorder(5,5,5,5);
        border7 = BorderFactory.createEmptyBorder(5,5,5,5);
        border8 = BorderFactory.createEmptyBorder(10,10,10,10);
        border9 = BorderFactory.createEmptyBorder(10,10,10,10);
        border10 = BorderFactory.createEmptyBorder(6,6,6,6);


        jPanelGenerateAnalyse.setLayout(borderLayout18);
      /*  jPanelSynapticProcesses.setLayout(borderLayout19);

        jButtonChanMechAdd.setText("Add Channel Mechanism");
        jButtonChanMechAdd.addActionListener(new java.awt.event.ActionListener()
        {
            public void actionPerformed(ActionEvent e)
            {
                jButtonChanMechAdd_actionPerformed(e);
            }
        });
        jButtonChanMechEdit.addActionListener(new java.awt.event.ActionListener()
        {
            public void actionPerformed(ActionEvent e)
            {
                jButtonChanMechEdit_actionPerformed(e);
            }
        });
        jButtonChanMechEdit.setText("Edit selected Channel Mechanism");
        jPanelChannelMechsInnerTab.setLayout(borderLayout20);
        jPanelChanMechsButtons.setBorder(BorderFactory.createEtchedBorder());
        jPanelChanMechsButtons.setLayout(borderLayout22);
        jPanelChannelMechsInnerTab.setBorder(null);
        jPanelChannelMechsMain.setBorder(BorderFactory.createEtchedBorder());
        jPanelChannelMechsMain.setLayout(borderLayout11);*/
        gridLayout1.setColumns(1);
        gridLayout1.setRows(2);
        jLabelChanMechTitle.setBorder(border2);
        jLabelChanMechTitle.setHorizontalAlignment(SwingConstants.CENTER);
        jLabelChanMechTitle.setText("The following are the channel mechanisms available to cells in this " +
    "project");
        jLabelSynapseTitle.setText("The following are the synaptic mechanisms available when building " +
    "networks");
        jLabelSynapseTitle.setHorizontalAlignment(SwingConstants.CENTER);
        jLabelSynapseTitle.setBorder(border2);
        jMenuItemZipUp.setText("Zip this project...");
        jMenuItemZipUp.addActionListener(new java.awt.event.ActionListener()
        {
            public void actionPerformed(ActionEvent e)
            {
                jMenuItemZipUp_actionPerformed(e);
            }
        });
        jLabelNumCellGroups.setEnabled(false);
        jLabelNumCellGroups.setText("Cell Groups:");
        //jPanelCellGroupClicks.setEditable(false);
        jLabelProjFileVersion.setEnabled(false);
        jLabelProjFileVersion.setText("Project File Version:");
        jTextFieldProjFileVersion.setEditable(false);
        jTextFieldProjFileVersion.setText("");
        jMenuItemUnzipProject.setText("Import zipped project...");

        jMenuItemUnzipProject.addActionListener(new java.awt.event.ActionListener()
        {
            public void actionPerformed(ActionEvent e)
            {
                jMenuItemUnzipProject_actionPerformed(e);
            }
        });


        jComboBoxView3DChoice.addPopupMenuListener(new javax.swing.event.PopupMenuListener()
        {
            public void popupMenuCanceled(PopupMenuEvent e)
            {
            }
            public void popupMenuWillBecomeInvisible(PopupMenuEvent e)
            {
            }
            public void popupMenuWillBecomeVisible(PopupMenuEvent e)
            {
                jComboBoxView3DChoice_popupMenuWillBecomeVisible(e);
            }
        });
        jPanelNetSetAA.setBorder(BorderFactory.createEtchedBorder());
        jPanelNetSetAA.setLayout(borderLayout24);
        jPanelAllNetSettings.setLayout(gridLayout2);
        gridLayout2.setColumns(1);
        gridLayout2.setRows(2);
        jPanelNetSetTable.setLayout(gridLayout3);
        jPanelSynapseButtons.setLayout(borderLayout21);
        jLabelNetConnSimpleConn.setMaximumSize(new Dimension(289, 25));
        jLabelNetConnSimpleConn.setMinimumSize(new Dimension(289, 25));
        jLabelNetConnSimpleConn.setPreferredSize(new Dimension(289, 25));
        jLabelNetConnSimpleConn.setHorizontalAlignment(SwingConstants.CENTER);
        jLabelNetConnSimpleConn.setText("Morphology Based Connections");
        jPanelNetSetControls.setBorder(BorderFactory.createEtchedBorder());
        jPanelNetSetControls.setMaximumSize(new Dimension(2147483647, 2147483647));
        jPanelNetSetControls.setMinimumSize(new Dimension(968, 64));
        jPanelNetSetControls.setPreferredSize(new Dimension(968, 64));
        jPanelNetSetControls.setLayout(borderLayout23);
        jPanelNetConnButtonsOnly.setMinimumSize(new Dimension(664, 35));
        jLabelNetSetAA.setMaximumSize(new Dimension(149, 25));
        jLabelNetSetAA.setMinimumSize(new Dimension(149, 25));
        jLabelNetSetAA.setPreferredSize(new Dimension(149, 25));
        jLabelNetSetAA.setHorizontalAlignment(SwingConstants.CENTER);
        jLabelNetSetAA.setText("Volume Based Connections");
        jButtonNetAAAdd.setEnabled(false);
        jButtonNetAAAdd.setText("Add Volume Based Conn");
        jButtonNetAAAdd.addActionListener(new java.awt.event.ActionListener()
        {
            public void actionPerformed(ActionEvent e)
            {
                jButtonNetAAAdd_actionPerformed(e);
            }
        });
        jPanelNetSetAAControls.setBorder(BorderFactory.createEtchedBorder());
        jPanelNetSetAAControls.setMinimumSize(new Dimension(153, 64));
        jPanelNetSetAAControls.setPreferredSize(new Dimension(153, 64));
        jPanelNetSetAAControls.setLayout(borderLayout25);
        jButtonCellGroupsEdit.setEnabled(false);
        jButtonCellGroupsEdit.setText("Edit selected Cell Group");
        jButtonCellGroupsEdit.addActionListener(new java.awt.event.ActionListener()
        {
            public void actionPerformed(ActionEvent e)
            {
                jButtonCellGroupsEdit_actionPerformed(e);
            }
        });
        jPanelNetSetAATable.setLayout(gridLayout4);
        jButtonNetAADelete.setEnabled(false);
        jButtonNetAADelete.setText("Delete selected Volume Based Conn");
        jButtonNetAADelete.addActionListener(new java.awt.event.ActionListener()
        {
            public void actionPerformed(ActionEvent e)
            {
                jButtonNetAADelete_actionPerformed(e);
            }
        });
        jButtonNetAAEdit.setEnabled(false);
        jButtonNetAAEdit.setText("Edit selected Volume Based Conn");
        jButtonNetAAEdit.addActionListener(new java.awt.event.ActionListener()
        {
            public void actionPerformed(ActionEvent e)
            {
                jButtonNetAAEdit_actionPerformed(e);
            }
        });
        jMenuItemViewProjSource.setText("View project file source");
        jMenuItemViewProjSource.addActionListener(new java.awt.event.ActionListener()
        {
            public void actionPerformed(ActionEvent e)
            {
                jMenuItemViewProjSource_actionPerformed(e);
            }
        });
        jButtonCellTypeViewCellInfo.setEnabled(false);
        jButtonCellTypeViewCellInfo.setText("View full Cell Info");
        jButtonCellTypeViewCellInfo.addActionListener(new java.awt.event.ActionListener()
        {
            public void actionPerformed(ActionEvent e)
            {
                jButtonCellTypeViewInfo_actionPerformed(e);
            }
        });
        jButtonToggleConsoleOut.setEnabled(true);
        jPanelExportGenesis.setLayout(borderLayout26);
        jLabelGenesisMain.setEnabled(false);
        jLabelGenesisMain.setBorder(border8);
        jLabelGenesisMain.setHorizontalAlignment(SwingConstants.CENTER);
        jLabelGenesisMain.setText("Generate code for the GENESIS simulation");
        jPanelExportGenesis.setDebugGraphicsOptions(0);
        jLabelSimulatorNeosimMain.setText("To be continued...");
        //jPanelExportNeosim.setLayout(borderLayout27);
        //jPanelExportNeosim.setBorder(BorderFactory.createEtchedBorder());
        jButtonCellTypeDelete.setEnabled(false);
        jButtonCellTypeDelete.setText("Delete Cell Type");
        jButtonCellTypeDelete.addActionListener(new java.awt.event.ActionListener()
        {
            public void actionPerformed(ActionEvent e)
            {
                jButtonCellTypeDelete_actionPerformed(e);
            }
        });
        jButtonCellTypeCompare.setEnabled(false);
        jButtonCellTypeCompare.setText("Compare Cell...");
        jButtonCellTypeCompare.addActionListener(new java.awt.event.ActionListener()
        {
            public void actionPerformed(ActionEvent e)
            {
                jButtonCellTypeCompare_actionPerformed(e);
            }
        });

        jPanelCellTypeMainInfo.setLayout(flowLayout1);
        jPanelCellTypeDetails.setDebugGraphicsOptions(0);
        jPanelCellTypeInfo.setBorder(BorderFactory.createEtchedBorder());
        jPanelCellTypeMainButtons.setMinimumSize(new Dimension(390, 115));
        jPanelCellTypeMainButtons.setPreferredSize(new Dimension(390, 115));
        jPanelCellTypeMainButtons.setLayout(flowLayout3);
        jButtonCellTypeCopy.setEnabled(false);
        jButtonCellTypeCopy.setText("Create copy of Cell Type");
        jButtonCellTypeCopy.addActionListener(new java.awt.event.ActionListener()
        {
            public void actionPerformed(ActionEvent e)
            {
                jButtonCellTypeCopy_actionPerformed(e);
            }
        });
        jButtonCellTypesMoveToOrigin.setEnabled(false);
        jButtonCellTypesMoveToOrigin.setText("Translate cell to origin");
        jButtonCellTypesMoveToOrigin.addActionListener(new java.awt.event.ActionListener()
        {
            public void actionPerformed(ActionEvent e)
            {
                jButtonCellTypesMoveToOrigin_actionPerformed(e);
            }
        });
        jButtonCellTypesConnect.setEnabled(false);
        jButtonCellTypesConnect.setText("Ensure segments connected to parents");
        jButtonCellTypesConnect.addActionListener(new java.awt.event.ActionListener()
        {
            public void actionPerformed(ActionEvent e)
            {
                jButtonCellTypesConnect_actionPerformed(e);
            }
        });
        jButtonCellTypesMakeSimpConn.setEnabled(false);
        jButtonCellTypesMakeSimpConn.setText("Make Simply Connected");
        jButtonCellTypesMakeSimpConn.addActionListener(new java.awt.event.ActionListener()
        {
            public void actionPerformed(ActionEvent e)
            {
                jButtonCellTypesMakeSimpConn_actionPerformed(e);
            }
        });
        jPanelCellTypeManageNumbers.setMinimumSize(new Dimension(600, 35));
        jPanelCellTypeManageNumbers.setPreferredSize(new Dimension(600, 35));
        flowLayout3.setHgap(0);
        flowLayout3.setVgap(0);
        jPanelCellTypesModify.setMinimumSize(new Dimension(700, 35));
        jPanelCellTypesModify.setPreferredSize(new Dimension(700, 35));
        jLabelMainLastModified.setEnabled(false);
        jLabelMainLastModified.setText("Last modified:");
        //jTextFieldMainLastModified.setEnabled(false);
        jTextFieldMainLastModified.setEditable(false);
        jMenuItemGlossary.setText("Glossary");
        jMenuItemGlossary.addActionListener(new java.awt.event.ActionListener()
        {
            public void actionPerformed(ActionEvent e)
            {
                jMenuItemGlossary_actionPerformed(e);
            }
        });
        jLabelExportMain.setText("Set the main simulation parameters and select the format in which " + "to generate the network");
        jPanelExportHeader.setMinimumSize(new Dimension(625, 35));
        jPanelExportHeader.setPreferredSize(new Dimension(625, 35));
        jPanelNeuronMainSettings.setLayout(gridBagLayout6);

        //jPanelNeuronMainSettings.setLayout(new GridLayout(4,1));
        jPanelHocFileButtons.setBorder(null);
        jPanelHocFileButtons.setMinimumSize(new Dimension(709, 50));
        jPanelHocFileButtons.setPreferredSize(new Dimension(473, 50));
        jTabbedPaneNeuron.setPreferredSize(new Dimension(478, 572));

        jButtonNeuroMLExportLevel1.setEnabled(false);
        jButtonNeuroMLExportLevel1.setText("Export all Cell Types to Level 1 NeuroML files (just anatomy)");

        jButtonNeuroMLExportLevel1.addActionListener(new java.awt.event.ActionListener()
        {
            public void actionPerformed(ActionEvent e)
            {
                jButtonNeuroMLExport_actionPerformed(e, NeuroMLConstants.NEUROML_LEVEL_1);
            }
        });

        jButtonNeuroMLExportLevel2.setEnabled(false);
        jButtonNeuroMLExportLevel2.setText("Export all Cell Types to Level 2 NeuroML files "
                                           +"(anatomy & Cell Mechanism placement)");
        jButtonNeuroMLExportLevel2.addActionListener(new java.awt.event.ActionListener()
        {
            public void actionPerformed(ActionEvent e)
            {
                jButtonNeuroMLExport_actionPerformed(e, NeuroMLConstants.NEUROML_LEVEL_2);
            }
        });


        jButtonNeuroMLExportCellLevel3.setEnabled(false);
        jButtonNeuroMLExportCellLevel3.setText("Export all Cell Types to Level 3 NeuroML files "
                                           +"(anatomy & channels & network aspects)");

        jButtonNeuroMLExportCellLevel3.addActionListener(new java.awt.event.ActionListener()
        {
            public void actionPerformed(ActionEvent e)
            {
                jButtonNeuroMLExport_actionPerformed(e, NeuroMLConstants.NEUROML_LEVEL_3);
            }
        });


        /*
    jButtonNeuroMLExportNetLevel3.setText("Export cell placement and network connections "
                                         +"to Level 3 NeuroML file");

        jButtonNeuroMLExportNetLevel3.addActionListener(new java.awt.event.ActionListener()
        {
            public void actionPerformed(ActionEvent e)
            {
                jButtonNeuroMLExportNet_actionPerformed(e);
            }
        });*/


        jLabelNeuroMLMain.setHorizontalAlignment(SwingConstants.CENTER);
        jLabelNeuroMLMain.setHorizontalTextPosition(SwingConstants.CENTER);
        jLabelNeuroMLMain.setText("The Cells included in this project can be exported to NeuroML/MorphML format");
        jPanelNeuroML.setLayout(borderLayout31);
        jPanelNeuroMLHeader.setMinimumSize(new Dimension(391, 35));
        jPanelNeuroMLHeader.setPreferredSize(new Dimension(391, 35));
        jPanelNeuroMLHeader.setLayout(gridLayout6);
        jPanelSimSettings.setEnabled(true);
        jPanelSimSettings.setDoubleBuffered(true);
        jLabelNeuroMLGeneratedFiles.setText("Generated files:");

        jButtonNeuroMLViewPlain.setEnabled(false);
        jButtonNeuroMLViewPlain.setText("View selected file");
        jButtonNeuroMLViewPlain.addActionListener(new java.awt.event.ActionListener()
        {
            public void actionPerformed(ActionEvent e)
            {
                jButtonMorphMLView_actionPerformed(e, false);
            }
        });

        jButtonNeuroMLViewFormatted.setEnabled(false);
        jButtonNeuroMLViewFormatted.setText("View selected file, formatted");
        jButtonNeuroMLViewFormatted.addActionListener(new java.awt.event.ActionListener()
        {
            public void actionPerformed(ActionEvent e)
            {
                jButtonMorphMLView_actionPerformed(e, true);
            }
        });


        jPanelNeuroMLView.setLayout(gridBagLayout3);

        jButtonGenesisGenerate.setEnabled(false);
        jButtonGenesisGenerate.setActionCommand("Create GENESIS files");
        jButtonGenesisGenerate.setText("Create GENESIS files");
        jButtonGenesisGenerate.addActionListener(new java.awt.event.ActionListener()
        {
            public void actionPerformed(ActionEvent e)
            {
                jButtonGenesisGenerate_actionPerformed(e);
            }
        });
        jButtonGenesisRun.setEnabled(false);
        jButtonGenesisRun.setText("Run GENESIS Simulation");
        jButtonGenesisRun.addActionListener(new java.awt.event.ActionListener()
        {
            public void actionPerformed(ActionEvent e)
            {
                jButtonGenesisRun_actionPerformed(e);
            }
        });
        //jPanelGenesisMain.setLayout(borderLayout32);
        jButtonGenesisView.setEnabled(false);
        jButtonGenesisView.setDoubleBuffered(false);
        jButtonGenesisView.setText("View:");
        jButtonGenesisView.addActionListener(new java.awt.event.ActionListener()
        {
            public void actionPerformed(ActionEvent e)
            {
                jButtonGenesisView_actionPerformed(e);
            }
        });
        jButtonRegionsEdit.setEnabled(false);
        jButtonRegionsEdit.setText("Edit Selected Region");
        jButtonRegionsEdit.addActionListener(new java.awt.event.ActionListener()
        {
            public void actionPerformed(ActionEvent e)
            {
                jButtonRegionsEdit_actionPerformed(e);
            }
        });
        jPanelNeuronExtraHoc.setLayout(gridLayout7);
        gridLayout7.setColumns(1);
        gridLayout7.setRows(1);
        //jPanelNeuronExtraHocBlock.setBorder(BorderFactory.createEtchedBorder());
        jPanelNeuronExtraHocBlock.setLayout(new BorderLayout());
        jPanelGenesisExtraBlock.setLayout(new BorderLayout());

        jTextAreaNeuronBlockDesc.setBorder(BorderFactory.createEtchedBorder());
        jTextAreaNeuronBlockDesc.setEditable(false);
        jTextAreaNeuronBlockDesc.setMinimumSize(new Dimension(600, 40));
        jTextAreaNeuronBlockDesc.setPreferredSize(new Dimension(600, 40));
        jTextAreaNeuronBlockDesc.setWrapStyleWord(true);
        jTextAreaNeuronBlockDesc.setLineWrap(true);

        jTextAreaGenesisBlockDesc.setBorder(BorderFactory.createEtchedBorder());
        jTextAreaGenesisBlockDesc.setEditable(false);
        jTextAreaGenesisBlockDesc.setMinimumSize(new Dimension(600, 40));
        jTextAreaGenesisBlockDesc.setPreferredSize(new Dimension(600, 40));
        jTextAreaGenesisBlockDesc.setWrapStyleWord(true);
        jTextAreaGenesisBlockDesc.setLineWrap(true);



        //jTextAreaNeuronBlockDesc.setHorizontalAlignment(SwingConstants.CENTER);
        jTextAreaNeuronBlockDesc.setText("This code will be included before creation of the cell " +
    "groups");


        jTextAreaNeuronBlock.setEnabled(false);
        jTextAreaNeuronBlock.setBorder(border5);
        jTextAreaNeuronBlock.setEditable(true);

        jTextAreaGenesisBlock.setEnabled(false);
        jTextAreaGenesisBlock.setBorder(BorderFactory.createEmptyBorder(5,5,5,5));
        jTextAreaGenesisBlock.setEditable(true);

        jScrollPaneNeuronBlock.setEnabled(false);

        jScrollPaneNeuronBlock.setMinimumSize(new Dimension(400, 430));
        jScrollPaneNeuronBlock.setPreferredSize(new Dimension(400, 430));



        jScrollPaneGenesisBlock.setMinimumSize(new Dimension(400, 430));
        jScrollPaneGenesisBlock.setPreferredSize(new Dimension(400, 430));

        ////jScrollPaneNeuronAfter.setMaximumSize(new Dimension(440, 450));
        ////jScrollPaneNeuronAfter.setMinimumSize(new Dimension(440, 450));
        ////jScrollPaneNeuronAfter.setPreferredSize(new Dimension(440, 450));
        jComboBoxNeuronFiles.setEnabled(false);

        jMenuItemPlotEquation.setText("Create plot from expression");
        jMenuItemPlotEquation.addActionListener(new java.awt.event.ActionListener()
        {
            public void actionPerformed(ActionEvent e)
            {
                jMenuItemPlotEquation_actionPerformed(e);
            }
        });

        jMenuItemPlotImport.setText("Import data for plot");
        jMenuItemPlotImport.addActionListener(new java.awt.event.ActionListener()
        {
            public void actionPerformed(ActionEvent e)
            {
                jMenuItemPlotImport_actionPerformed(e);
            }
        });

        jMenuItemCondorMonitor.setText("Condor Monitor");
        jMenuItemCondorMonitor.addActionListener(new java.awt.event.ActionListener()
        {
            public void actionPerformed(ActionEvent e)
            {
                jMenuItemCondorMonitor_actionPerformed(e);
            }
        });




        jMenuItemMPIMonitor.setText("Parallel Monitor");
        jMenuItemMPIMonitor.addActionListener(new java.awt.event.ActionListener()
        {
            public void actionPerformed(ActionEvent e)
            {
                jMenuItemMPIMonitor_actionPerformed(e);
            }
        });





        jRadioButtonNeuronSimDontRecord.setEnabled(false);
        jRadioButtonNeuronSimDontRecord.setSelected(true);
        jRadioButtonNeuronSimSaveToFile.setEnabled(false);
        jRadioButtonNeuronSimSaveToFile.setSelected(false);
        jLabelSimRef.setEnabled(false);
        jTextFieldSimRef.setEnabled(false);
        jCheckBoxSpecifySimRef.setEnabled(false);
        jCheckBoxSpecifySimRef.setText("Overwrite");
        jCheckBoxNeuronSaveHoc.setEnabled(false);
        jCheckBoxNeuronSaveHoc.setText("Save copy of hoc files");
        jLabelSimDefDur.setEnabled(false);
        jLabelSimDefDur.setForeground(Color.black);
        jTextFieldSimDefDur.setEnabled(false);
        jLabelSimDT.setEnabled(false);
        jTextFieldSimDT.setEnabled(false);
        jLabelSimSummary.setEnabled(false);
        jLabelSimSummary.setBorder(border10);
        jLabelSimSummary.setHorizontalAlignment(SwingConstants.CENTER);

        jButtonNeuronCreateCondor.setEnabled(false);
        jButtonNeuronCreateCondor.setText("Create files for sending to Condor");
        jButtonNeuronCreateCondor.addActionListener(new java.awt.event.ActionListener()
        {
            public void actionPerformed(ActionEvent e)
            {
                jButtonNeuronCreateCondor_actionPerformed(e);
            }
        });


        jButtonNeuronCreateMPI.setEnabled(false);
        jButtonNeuronCreateMPI.setText("Create files for MPI based execution");
        jButtonNeuronCreateMPI.addActionListener(new java.awt.event.ActionListener()
        {
            public void actionPerformed(ActionEvent e)
            {
                jButtonNeuronCreateMPI_actionPerformed(e);
            }
        });



        jComboBoxGenesisFiles.setEnabled(false);

        jPanelSimGeneral.setLayout(borderLayout33);
        jLabelNeuronMainLabel.setBorder(border7);
        jLabelNeuronMainLabel.setHorizontalAlignment(SwingConstants.CENTER);
        jLabelNeuronMainLabel.setText("Generate code for the NEURON simulator");
        jLabelSimulationGlobRa.setText("Default specific axial resistance:");
        jTextFieldSimulationGlobRa.setText("1");
        jTextFieldSimulationGlobRa.setColumns(6);
        jLabelSimulationGlobCm.setText("Default specific membrane capacitance:");
        jTextFieldSimulationGlobCm.setText("2");
        jTextFieldSimulationGlobCm.setColumns(6);

        /*
        jPanelGenesisExtraBefore.setLayout(flowLayout7);
        jPanelGenesisExtraBefore.setBorder(BorderFactory.createEtchedBorder());

        jLabelGenesisExtraBefore.setHorizontalAlignment(SwingConstants.CENTER);
        jLabelGenesisExtraBefore.setText("This code will be included before creation of the cell " +
    "groups");
        jTextAreaGenesisAfter.setEnabled(false);
        jTextAreaGenesisAfter.setBorder(border6);
        jPanelGenesisExtraAfter.setBorder(BorderFactory.createEtchedBorder());
        jPanelGenesisExtraAfter.setLayout(flowLayout6);

        */
        jPanelGenesisExtra.setLayout(gridLayout8);
        gridLayout8.setColumns(2);
        gridLayout8.setRows(1);
        /*
        jScrollPaneGenesisAfter.setMaximumSize(new Dimension(440, 450));
        jScrollPaneGenesisAfter.setMinimumSize(new Dimension(440, 450));
        jScrollPaneGenesisAfter.setPreferredSize(new Dimension(440, 450));
        jLabelGenesisExtraAfter.setBorder(border4);
        jLabelGenesisExtraAfter.setHorizontalAlignment(SwingConstants.CENTER);
        jLabelGenesisExtraAfter.setText("This code will be included after creation of the cell " +
    "groups");
        jScrollPaneGenesisBefore.setEnabled(false);
        jScrollPaneGenesisBefore.setMaximumSize(new Dimension(440, 450));
        jScrollPaneGenesisBefore.setMinimumSize(new Dimension(440, 450));
        jScrollPaneGenesisBefore.setPreferredSize(new Dimension(440, 450));
        jTextAreaGenesisBefore.setEditable(true);
        jTextAreaGenesisBefore.setBorder(border5);
        jTextAreaGenesisBefore.setEnabled(false);
*/
        jPanelSimulationParams.setBorder(null);
        jPanelSimulationParams.setLayout(borderLayout34);
        jPanelSimulationGlobal.setBorder(BorderFactory.createEtchedBorder());
        jPanelSimulationGlobal.setMaximumSize(new Dimension(790, 60));
        jPanelSimulationGlobal.setMinimumSize(new Dimension(790, 60));
        jPanelSimulationGlobal.setPreferredSize(new Dimension(790, 60));
        jPanelSimulationGlobal.setLayout(gridBagLayout5);
        jButton3DPrevSims.setEnabled(false);
        jButton3DPrevSims.setSelected(false);
        jButton3DPrevSims.setText("Previous Simulations...");
        jButton3DPrevSims.addActionListener(new java.awt.event.ActionListener()
        {
            public void actionPerformed(ActionEvent e)
            {
                jButton3DPrevSims_actionPerformed(e);
            }
        });
        jLabelSimulationGlobRm.setText("Default specific membrane resistance:");
        jTextFieldSimulationGlobRm.setText("50");
        jTextFieldSimulationGlobRm.setColumns(6);
        jLabelSimulationInitVm.setText("Default initial membrane potential:");
        jTextFieldSimulationInitVm.setText("-60");
        jTextFieldSimulationInitVm.setColumns(6);
        jLabelSimulationVLeak.setText("Default membrane leakage potential:");
        jTextFieldSimulationVLeak.setText("-54.6");
        jTextFieldSimulationVLeak.setColumns(6);
        jCheckBoxGenesisSymmetric.setEnabled(false);
        jCheckBoxGenesisSymmetric.setHorizontalAlignment(SwingConstants.CENTER);
        jCheckBoxGenesisSymmetric.setText("Symmetric compartments");
        jPanelGenesisSettings.setLayout(borderLayout35);
        borderLayout35.setHgap(10);
        borderLayout35.setVgap(10);
     //   jButtonSimulationRecord.setEnabled(false);
     //   jButtonSimulationRecord.setSelected(false);
    //    jButtonSimulationRecord.setText("Change...");

        jCheckBoxNeuronShowShapePlot.setEnabled(false);// jPanelSimulationParams.add(jPanelSimulationWhatToRec,  BorderLayout.NORTH);
        this.jCheckBoxNeuronNoGraphicsMode.setEnabled(false);
        jCheckBoxGenesisNoGraphicsMode.setEnabled(false);

        jCheckBoxNeuronShowShapePlot.setText("Show 3D potential plot");
        jCheckBoxNeuronNoGraphicsMode.setText("No GUI mode");
        jCheckBoxGenesisNoGraphicsMode.setText("No GUI mode");

   //     jPanelSimulationWhatToRec.add(jButtonSimulationRecord, null);
        jRadioButtonSimSomaOnly.setSelected(true);
        jRadioButtonSimSomaOnly.setText("Soma of each cell");
        jLabelSimWhatToRecord.setText("Record potential at:");
        jLabelSimWhatToRecord.setEnabled(false);
        jRadioButtonSimAllSegments.setText("Every segment of every cell");
        jRadioButtonSimAllSections.setText("Every section of every cell");
        jRadioButtonGenesisPhy.setEnabled(false);
        jRadioButtonGenesisPhy.setDoubleBuffered(false);
        jRadioButtonGenesisPhy.setSelected(true);
        jRadioButtonGenesisPhy.setText("Generate in Physiological units");
        jRadioButtonGenesisSI.setEnabled(false);
        jRadioButtonGenesisSI.setText("Generate in SI units");
        jPanelCellMechanisms.setLayout(borderLayout28);
        JLabelMechanismMain.setBorder(border9);
        JLabelMechanismMain.setHorizontalAlignment(SwingConstants.CENTER);
        JLabelMechanismMain.setText("The following mechanisms are available for placing on the Cells in " +
    "the project");
        jPanelMechanismMain.setBorder(BorderFactory.createEtchedBorder());
        jPanelMechanismMain.setLayout(borderLayout36);
        jButtonMechanismDelete.setText("Delete selected Cell Mechanism");
    jButtonMechanismDelete.addActionListener(new java.awt.event.ActionListener() {
      public void actionPerformed(ActionEvent e) {
        jButtonMechanismDelete_actionPerformed(e);
      }
    });
        jButtonMechanismEdit.setText("Edit selected Cell Mechanism");
    jButtonMechanismEdit.addActionListener(new java.awt.event.ActionListener() {
      public void actionPerformed(ActionEvent e) {
        jButtonMechanismEdit_actionPerformed(e);
      }
    });
    jButtonMechanismAbstract.setEnabled(false);
    jButtonMechanismTemplateCML.setEnabled(false);
    jButtonMechanismFileBased.setEnabled(false);
    jButtonMechanismNewCML.setEnabled(false);
    jButtonMechanismAbstract.setText("Add Abstracted Cell Mechanism");
    jButtonMechanismTemplateCML.setText("Add ChannelML from Template");
    jButtonMechanismAbstract.addActionListener(new java.awt.event.ActionListener()
    {
        public void actionPerformed(ActionEvent e)
        {
            jButtonMechanismAdd_actionPerformed(e);
        }
        });
        jPanelMechanismLabel.setLayout(borderLayout37);
        jMenuItemUnits.setText("Units used");
        jPanelGenesisChoices.setLayout(borderLayout38);
        jButtonMechanismFileBased.setText("Create File Based Mechanism");
        jButtonMechanismNewCML.setText("Create ChannelML Mechanism");
        jButtonMechanismFileBased.addActionListener(new java.awt.event.ActionListener()
        {
            public void actionPerformed(ActionEvent e)
            {
                jButtonMechanismFileBased_actionPerformed(e);
            }
        });
        jButtonMechanismNewCML.addActionListener(new java.awt.event.ActionListener()
        {
            public void actionPerformed(ActionEvent e)
            {
                jButtonMechanismNewCML_actionPerformed(e);
            }
        });

        this.jButtonMechanismTemplateCML.addActionListener(new java.awt.event.ActionListener()
        {
            public void actionPerformed(ActionEvent e)
            {
                jButtonMechanismTemplateCML_actionPerformed(e);
            }
        });


        jLabelSimulationTemp.setText("Temperature:");
        jTextFieldSimulationTemperature.setText("25");
        jTextFieldSimulationTemperature.setColumns(6);
        jCheckBoxGenesisComments.setEnabled(false);
        jCheckBoxGenesisComments.setText("Generate comments");

        jCheckBoxGenesisShapePlot.setEnabled(false);
        jCheckBoxGenesisShapePlot.setText("Show 3D potential plot");


        jProgressBarGenerate.setEnabled(false);
        jProgressBarGenerate.setMinimumSize(new Dimension(200, 21));
        jProgressBarGenerate.setPreferredSize(new Dimension(300, 21));
        jProgressBarGenerate.setMinimum(0);
        jProgressBarGenerate.setStringPainted(true);
        jLabelGenesisNumMethod.setEnabled(false);
        jLabelGenesisNumMethod.setText("Num integration method");
        jButtonGenesisNumMethod.setEnabled(false);
        jButtonGenesisNumMethod.setText("Change...");
        jButtonGenesisNumMethod.addActionListener(new java.awt.event.ActionListener()
        {
            public void actionPerformed(ActionEvent e)
            {
                jButtonGenesisNumMethod_actionPerformed(e);
            }
        });
        jMenuItemCopyProject.setText("Copy project (Save As)...");
        jMenuItemCopyProject.addActionListener(new java.awt.event.ActionListener()
        {
            public void actionPerformed(ActionEvent e)
            {
                jMenuItemCopyProject_actionPerformed(e);
            }
        });
        jPanelSimPlot.setBorder(BorderFactory.createEtchedBorder());

        //jPanelSimPlot.setMinimumSize(new Dimension(300, 250));
        jPanelSimPlot.setPreferredSize(new Dimension(300,350));
        jPanelStims.setPreferredSize(new Dimension(300,300));
        jPanelStims.setMaximumSize(new Dimension(300,300));
        jPanelSimPlot.setLayout(borderLayout42);
        jPanelInputOutput.setLayout(borderLayout39);
        jPanelSimWhatToRecord.setLayout(borderLayout40);
        jPanelSimWhatToRecord.setBorder(BorderFactory.createEtchedBorder());
        jPanelSimValsInput.setLayout(borderLayout41);


        jTextFieldSimTotalTimeUnits.setEditable(false);
        jTextFieldSimTotalTimeUnits.setText("");
        jTextFieldSimTotalTimeUnits.setColumns(6);
        jTextFieldSimDTUnits.setEditable(false);
        jTextFieldSimDTUnits.setSelectionStart(11);
        jTextFieldSimDTUnits.setText("");
        jTextFieldSimDTUnits.setColumns(6);
        jButtonSimPlotAdd.setText("Specify new variable to plot/save");
        jButtonSimPlotAdd.addActionListener(new java.awt.event.ActionListener()
        {
            public void actionPerformed(ActionEvent e)
            {
                jButtonSimPlotAdd_actionPerformed(e);
            }
        });
        jButtonSimPlotDelete.setText("Delete selected plot");
        jButtonSimPlotDelete.addActionListener(new java.awt.event.ActionListener()
        {
            public void actionPerformed(ActionEvent e)
            {
                jButtonSimPlotDelete_actionPerformed(e);
            }
        });
        jButtonSimPlotEdit.setToolTipText("");
        jButtonSimPlotEdit.setActionCommand("jButtonSimPlotEdit");
        jButtonSimPlotEdit.setText("Edit selected plot");
        jButtonSimPlotEdit.addActionListener(new java.awt.event.ActionListener()
        {
            public void actionPerformed(ActionEvent e)
            {
                jButtonSimPlotEdit_actionPerformed(e);
            }
        });
        jButtonSimStimAdd.setText("Add electrophysiological input");
        jButtonSimStimAdd.addActionListener(new java.awt.event.ActionListener()
        {
            public void actionPerformed(ActionEvent e)
            {
                jButtonSimStimAdd_actionPerformed(e);
            }
        });
        jButtonSimStimDelete.setText("Delete selected input");
        jButtonSimStimDelete.addActionListener(new java.awt.event.ActionListener()
        {
            public void actionPerformed(ActionEvent e)
            {
                jButtonSimStimDelete_actionPerformed(e);
            }
        });
        jButtonSimStimEdit.setText("Edit selected input");
        jButtonSimStimEdit.addActionListener(new java.awt.event.ActionListener()
        {
            public void actionPerformed(ActionEvent e)
            {
                jButtonSimStimEdit_actionPerformed(e);
            }
        });
        jTextFieldSimUnitGlobRa.setEditable(false);
        jTextFieldSimUnitGlobRa.setText("");
        jTextFieldSimUnitGlobRa.setColumns(7);
        jTextFieldSimUnitGlotCm.setEditable(false);
        jTextFieldSimUnitGlotCm.setText("");
        jTextFieldSimUnitGlotCm.setColumns(7);

        jTextFieldSimUnitGlobRm.setEditable(false);
        jTextFieldSimUnitGlobRm.setText("");
        jTextFieldSimUnitGlobRm.setColumns(7);
        jTextFieldSimUnitInitVm.setEditable(false);
        jTextFieldSimUnitInitVm.setText("");
        jTextFieldSimUnitInitVm.setColumns(7);
        jTextFieldSimUnitVLeak.setEditable(false);
        jTextFieldSimUnitVLeak.setText("");
        jTextFieldSimUnitVLeak.setColumns(7);
        jTextFieldSimUnitTemp.setDebugGraphicsOptions(0);
        jTextFieldSimUnitTemp.setDoubleBuffered(false);
        jTextFieldSimUnitTemp.setEditable(false);
        jTextFieldSimUnitTemp.setText("");
        jTextFieldSimUnitTemp.setColumns(7);

        jMenuItemHelp.setText("Help");
        jMenuItemHelp.addActionListener(new java.awt.event.ActionListener()
        {
            public void actionPerformed(ActionEvent e)
            {
                jMenuItemHelp_actionPerformed(e);
            }
        });
        jCheckBoxNeuronComments.setEnabled(false);
        jCheckBoxNeuronComments.setText("Generate comments");
        jButtonCellTypeBioPhys.setEnabled(false);
        jButtonCellTypeBioPhys.setText("Edit Init Potential");
        jButtonCellTypeBioPhys.addActionListener(new java.awt.event.ActionListener()
        {
            public void actionPerformed(ActionEvent e)
            {
                jButtonCellTypeBioPhys_actionPerformed(e);
            }
        });



        jButtonCellTypeEditDesc.setEnabled(false);
        jButtonCellTypeEditDesc.setText("Edit Description");
        jButtonCellTypeEditDesc.addActionListener(new java.awt.event.ActionListener()
        {
            public void actionPerformed(ActionEvent e)
            {
                jButtonCellTypeEditDesc_actionPerformed(e);
            }
        });
        jButtonCellTypeOtherProject.setEnabled(false);
        jButtonCellTypeOtherProject.setDoubleBuffered(false);
        jButtonCellTypeOtherProject.setText("Add Cell Type from another Project..");
        jButtonCellTypeOtherProject.addActionListener(new java.awt.event.ActionListener()
        {
            public void actionPerformed(ActionEvent e)
            {
                jButtonCellTypeOtherProject_actionPerformed(e);
            }
        });
        jMenuItemJava.setText("Java properties");
        jMenuItemJava.addActionListener(new java.awt.event.ActionListener()
        {
            public void actionPerformed(ActionEvent e)
            {
                jMenuItemJava_actionPerformed(e);
            }
        });
        jMenuProject.setText("Project");
        jMenuItemGenNetwork.setText("Generate Positions & Network");
        jMenuItemGenNetwork.addActionListener(new java.awt.event.ActionListener()
        {
            public void actionPerformed(ActionEvent e)
            {
                jMenuItemGenNetwork_actionPerformed(e);
            }
        });
        jMenuItemGenNeuron.setText("Generate NEURON");
        jMenuItemGenNeuron.addActionListener(new java.awt.event.ActionListener()
        {
            public void actionPerformed(ActionEvent e)
            {
                jMenuItemGenNeuron_actionPerformed(e);
            }
        });

        jMenuItemGenGenesis.setText("Generate GENESIS");
        jMenuItemGenGenesis.addActionListener(new java.awt.event.ActionListener()
        {
            public void actionPerformed(ActionEvent e)
            {
                jMenuItemGenGenesis_actionPerformed(e);
            }
        });

        jMenuItemPrevSims.setText("List Previous Simulations");
        jMenuItemPrevSims.addActionListener(new java.awt.event.ActionListener()
        {
            public void actionPerformed(ActionEvent e)
            {
                jMenuItemPrevSims_actionPerformed(e);
            }
        });

        jMenuItemDataSets.setText("Data Set Manager");
        jMenuItemDataSets.addActionListener(new java.awt.event.ActionListener()
        {
            public void actionPerformed(ActionEvent e)
            {
                jMenuItemDataSets_actionPerformed(e);
            }
        });



        jPanelNeuroMLView.add(jLabelNeuroMLGeneratedFiles,
                              new GridBagConstraints(0, 1, 1, 1, 0.0, 0.0,
                                                     GridBagConstraints.CENTER,
                                                     GridBagConstraints.NONE,
                                                     new Insets(6, 0, 12, 0), 0, 0));

        jPanelNeuroMLView.add(jComboBoxNeuroML,
                              new GridBagConstraints(0, 2, 1, 1, 1.0, 0.0,
                                                     GridBagConstraints.CENTER,
                                                     GridBagConstraints.NONE,
                                                     new Insets(6, 0, 12, 0), 0, 0));

        jPanelNeuroMLView.add(jButtonNeuroMLViewPlain,
                              new GridBagConstraints(0, 3, 1, 1, 0.0, 0.0,
                                                     GridBagConstraints.CENTER,
                                                     GridBagConstraints.NONE,
                                                     new Insets(6, 0, 12, 0), 0, 0));

        jPanelNeuroMLView.add(jButtonNeuroMLViewFormatted,
                              new GridBagConstraints(0, 4, 1, 1, 0.0, 0.0,
                                                     GridBagConstraints.CENTER,
                                                     GridBagConstraints.NONE,
                                                     new Insets(6, 0, 12, 0), 0, 0));

        jPanelNeuroMLView.add(jComboBoxNeuroMLComps,
                              new GridBagConstraints(0, 5, 1, 1, 0.0, 0.0,
                                                     GridBagConstraints.CENTER,
                                                     GridBagConstraints.NONE,
                                                     new Insets(6, 0, 12, 0), 0, 0));

        jPanelNeuroMLView.add(jTextAreaNeuroMLCompsDesc,
                              new GridBagConstraints(0, 6, 1, 1, 0.0, 0.0,
                                                     GridBagConstraints.CENTER,
                                                     GridBagConstraints.NONE,
                                                     new Insets(6, 0, 12, 0), 0, 0));


        jPanelNeuroMLView.add(jButtonNeuroMLExportLevel1,
                              new GridBagConstraints(0, 7, 1, 1, 0.0, 0.0,
                                                     GridBagConstraints.CENTER,
                                                     GridBagConstraints.NONE,
                                                     new Insets(6, 0, 12, 0), 0, 0));

        jPanelNeuroMLView.add(jButtonNeuroMLExportLevel2,
                              new GridBagConstraints(0, 8, 1, 1, 0.0, 0.0,
                                                     GridBagConstraints.CENTER,
                                                     GridBagConstraints.NONE,
                                                     new Insets(6, 0, 12, 0), 0, 0));

        jPanelNeuroMLView.add(jButtonNeuroMLExportCellLevel3,
                              new GridBagConstraints(0, 9, 1, 1, 0.0, 0.0,
                                                     GridBagConstraints.CENTER,
                                                     GridBagConstraints.NONE,
                                                     new Insets(6, 0, 12, 0), 0, 0));

/*
        jPanelNeuroMLView.add(jButtonNeuroMLExportNetLevel3,
                              new GridBagConstraints(0, 10, 1, 1, 0.0, 0.0,
                                                     GridBagConstraints.CENTER,
                                                     GridBagConstraints.NONE,
                                                     new Insets(6, 0, 150, 0), 0, 0));*/




        jPanelCellTypeMainButtons.add(jPanelCellTypesButtonsInfo, null);
        jPanelCellTypeMainButtons.add(jPanelCellTypesModify, null);
        jPanelCellTypesModify.add(jButtonCellTypesMoveToOrigin, null);
        jPanelCellTypesModify.add(jButtonCellTypesConnect, null);
        jPanelCellTypesModify.add(jButtonCellTypesMakeSimpConn, null);
        jPanelCellTypeMainButtons.add(jPanelCellTypeManageNumbers, null);
        jPanelNetSetAAControls.add(jPanelNetSetAAConButtons,  BorderLayout.SOUTH);
        jPanelNetSetAAConButtons.add(jButtonNetAAAdd, null);
        jPanelNetSetAAConButtons.add(jButtonNetAAEdit, null);
        jPanelNetSetAAConButtons.add(jButtonNetAADelete, null);
        jPanelNetSetControls.add(jLabelNetConnSimpleConn, BorderLayout.NORTH);
        ///////jPanelSynapseButtonsOnly.add(jButtonSynapseAdd, null);
        /////jPanelChanMechsButtonsOnly.add(jButtonChanMechAdd, null);
        ////////jPanelChanMechsButtonsOnly.add(jButtonChanMechEdit, null);
        jPanelNetSetControls.add(jPanelNetConnButtonsOnly, BorderLayout.CENTER);
        jPanelNetConnButtonsOnly.add(jButtonNetSetAddNew, null);
        jPanelNetConnButtonsOnly.add(jButtonNetConnEdit, null);
        jPanelNetConnButtonsOnly.add(jButtonNetConnDelete, null);

        jPanelNetSetTable.add(jScrollPaneNetConnects, BorderLayout.CENTER);

        jPanelNetSetAATable.add(jScrollPaneAAConns, BorderLayout.CENTER);

        jPanelNetSetSimple.setBorder(BorderFactory.createEtchedBorder());
        jPanelNetSetSimple.setLayout(borderLayout13);

        jPanelNetSetSimple.add(jPanelNetSetTable, BorderLayout.CENTER);

        jPanelNetSetSimple.add(jPanelNetSetControls, BorderLayout.NORTH);
        //jPanelNetSetMain.add(jPanelNetSetTable, BorderLayout.NORTH);

        jScrollPaneNetConnects.getViewport().add(jTableNetConns, null);
        jScrollPaneAAConns.getViewport().add(jTableAAConns, null);



        jPanelAllNetSettings.add(jPanelNetSetSimple);

        jPanelAllNetSettings.add(jPanelNetSetAA, null);
        jPanelGenerateAnalyse.add(jPanelGenerateComboBoxes, BorderLayout.NORTH);
        jPanelGenerateAnalyse.add(jPanelGenerateAnalyseButtons,  BorderLayout.CENTER);

        jPanelGenerateAnalyseButtons.add(jButtonAnalyseCellDensities, null);
        jPanelGenerateAnalyseButtons.add(jButtonAnalyseNumConns, null);
        jPanelGenerateAnalyseButtons.add(jButtonAnalyseConnLengths, null);


        imageNewProject = new ImageIcon(ucl.physiol.neuroconstruct.gui.MainFrame.class.getResource(
            "New24.gif"));
        imageOpenProject = new ImageIcon(ucl.physiol.neuroconstruct.gui.MainFrame.class.getResource(
            "Open24.gif"));
        imageSaveProject = new ImageIcon(ucl.physiol.neuroconstruct.gui.MainFrame.class.getResource(
            "Save24.gif"));
        imageCloseProject = new ImageIcon(ucl.physiol.neuroconstruct.gui.MainFrame.class.
                                          getResource("Close24.gif"));
        imageProjectPrefs = new ImageIcon(ucl.physiol.neuroconstruct.gui.MainFrame.class.
                                          getResource("Preferences24.gif"));
        imageNeuroConstruct = new ImageIcon(ucl.physiol.neuroconstruct.gui.MainFrame.class.
                                            getResource("small.png"));

        imageTips = new ImageIcon(ucl.physiol.neuroconstruct.gui.MainFrame.class.
                                          getResource("ToggleTips24.GIF"));
        imageNoTips = new ImageIcon(ucl.physiol.neuroconstruct.gui.MainFrame.class.
                                          getResource("ToggleTipsNone24.GIF"));

        imageConsoleOut = new ImageIcon(ucl.physiol.neuroconstruct.gui.MainFrame.class.
                                                getResource("ConsoleOutOn24.GIF"));
        imageNoConsoleOut = new ImageIcon(ucl.physiol.neuroconstruct.gui.MainFrame.class.
                                                getResource("ConsoleOutOff24.GIF"));


        contentPane = (JPanel)this.getContentPane();
        border1 = BorderFactory.createBevelBorder(BevelBorder.LOWERED, new Color(228, 228, 228),
                                                  new Color(228, 228, 228), new Color(93, 93, 93),
                                                  new Color(134, 134, 134));
        contentPane.setLayout(borderLayout1);
        this.setSize(new Dimension(964, 744));
        this.setTitle("neuroConstruct v"+GeneralProperties.getVersionNumber());
        statusBar.setBorder(BorderFactory.createEtchedBorder());
        statusBar.setText(" ");
        jMenuFile.setText("File");
        jMenuFileExit.setText("Exit");
        jMenuFileExit.addActionListener(new ActionListener()
        {
            public void actionPerformed(ActionEvent e)
            {
                jMenuFileExit_actionPerformed(e);
            }
        });
        jMenuHelp.setText("Help");
        jMenuHelpAbout.setText("About");
        jMenuHelpAbout.addActionListener(new ActionListener()
        {
            public void actionPerformed(ActionEvent e)
            {
                jMenuHelpAbout_actionPerformed(e);
            }
        });

        jMenuItemUnits.addActionListener(new ActionListener()
        {
            public void actionPerformed(ActionEvent e)
            {
                jMenuItemUsed_actionPerformed(e);
            }
        });

        jButtonOpenProject.addActionListener(new java.awt.event.ActionListener()
        {
            public void actionPerformed(ActionEvent e)
            {
                jButtonOpenProject_actionPerformed(e);
            }
        });
        jButtonOpenProject.setEnabled(true);
        jButtonOpenProject.setIcon(imageOpenProject);
        jButtonOpenProject.setMargin(new Insets(0, 0, 0, 0));

        jButtonSaveProject.setIcon(imageSaveProject);
        jButtonSaveProject.setMargin(new Insets(0, 0, 0, 0));
        jButtonSaveProject.addActionListener(new java.awt.event.ActionListener()
        {
            public void actionPerformed(ActionEvent e)
            {
                jButtonSaveProject_actionPerformed(e);
            }
        });



        jButtonValidate.setText("Validate");
        jButtonValidate.addActionListener(new java.awt.event.ActionListener()
        {
            public void actionPerformed(ActionEvent e)
            {
                jButtonValidate_actionPerformed(e);
            }
        });


        jButtonSaveProject.setEnabled(false);

        jButtonCloseProject.setIcon(imageCloseProject);
        jButtonCloseProject.setMargin(new Insets(0, 0, 0, 0));
        jButtonCloseProject.addActionListener(new java.awt.event.ActionListener()
        {
            public void actionPerformed(ActionEvent e)
            {
                jButtonCloseProject_actionPerformed(e);
            }
        });
        jButtonCloseProject.setEnabled(false);

        jButtonPreferences.setIcon(imageProjectPrefs);
        jButtonPreferences.setMargin(new Insets(0, 0, 0, 0));
        jButtonPreferences.addActionListener(new java.awt.event.ActionListener()
        {
            public void actionPerformed(ActionEvent e)
            {
                jButtonPreferences_actionPerformed(e);
            }
        });
        jButtonPreferences.setEnabled(false);

        //jButtonToggleTips.setIcon(imageTips);

        if (ToolTipManager.sharedInstance().isEnabled())
        {
            jButtonToggleTips.setIcon(imageTips);
        }
        else
        {
            jButtonToggleTips.setIcon(imageNoTips);
        }

        jButtonToggleTips.setMargin(new Insets(0, 0, 0, 0));
        jButtonToggleTips.addActionListener(new java.awt.event.ActionListener()
        {
            public void actionPerformed(ActionEvent e)
            {
                jButtonToggleTips_actionPerformed(e);
            }
        });
        jButtonToggleTips.setToolTipText("Turn off/on Tool Tips");

        jButtonToggleConsoleOut.setIcon(imageConsoleOut);
        jButtonToggleConsoleOut.setMargin(new Insets(0, 0, 0, 0));
        jButtonToggleConsoleOut.addActionListener(new java.awt.event.ActionListener()
        {
            public void actionPerformed(ActionEvent e)
            {
                jButtonToggleConsoleOut_actionPerformed(e);
            }
        });
        jButtonToggleConsoleOut.setToolTipText("Turn on/off console output. Note: turning this on can diminish application performance");


        contentPane.setBorder(BorderFactory.createEtchedBorder());
        jToolBar.setBorder(BorderFactory.createEtchedBorder());
        jMenuItemFileOpen.setText("Open Project...");
        jMenuItemFileOpen.addActionListener(new java.awt.event.ActionListener()
        {
            public void actionPerformed(ActionEvent e)
            {
                jMenuItemFileOpen_actionPerformed(e);
            }
        });
        jMenuItemSaveProject.setEnabled(false);
        jMenuItemSaveProject.setActionCommand("Save Project");
        jMenuItemSaveProject.setText("Save Project");
        jMenuItemSaveProject.addActionListener(new ActionListener()
        {
            public void actionPerformed(ActionEvent e)
            {
                jMenuItemSaveProject_actionPerformed(e);
            }
        });

        jMenuSettings.setText("Settings");
        jMenuItemProjProperties.setEnabled(false);
        jMenuItemProjProperties.setText("Project properties");
        jMenuItemProjProperties.addActionListener(new java.awt.event.ActionListener()
        {
            public void actionPerformed(ActionEvent e)
            {
                jMenuItemProjProperties_actionPerformed(e);
            }
        });
        jMenuTools.setText("Tools");
        jMenuItemNmodlEditor.setText("nmodlEditor...");
        jMenuItemNmodlEditor.setEnabled(false);
        jMenuItemNmodlEditor.addActionListener(new java.awt.event.ActionListener()
        {
            public void actionPerformed(ActionEvent e)
            {
                jMenuItemNmodlEditor_actionPerformed(e);
            }
        });
        jMenuItemNewProject.setText("New Project...");
        jMenuItemNewProject.addActionListener(new java.awt.event.ActionListener()
        {
            public void actionPerformed(ActionEvent e)
            {
                jMenuItemNewProject_actionPerformed(e);
            }
        });
        jButtonNewProject.setText("");
        jButtonNewProject.addActionListener(new java.awt.event.ActionListener()
        {
            public void actionPerformed(ActionEvent e)
            {
                jButtonNewProject_actionPerformed(e);
            }
        });
        jButtonNewProject.setEnabled(true);
        jButtonNewProject.setMaximumSize(new Dimension(35, 33));
        jButtonNewProject.setIcon(imageNewProject);
        jButtonNewProject.setMargin(new Insets(0, 0, 0, 0));
        //jPanelProjInfo.setAlignmentX( (float) 0.5);
        //jPanelProjInfo.setDebugGraphicsOptions(0);
        jPanelProjInfo.setLayout(gridBagLayout4);
        jTextAreaProjDescription.setBorder(BorderFactory.createEtchedBorder());
        jTextAreaProjDescription.setMinimumSize(new Dimension(300, 450));
        jTextAreaProjDescription.setPreferredSize(new Dimension(300, 450));
        jTextAreaProjDescription.setEditable(false);
        jTextAreaProjDescription.setText(welcomeText);
        jTextAreaProjDescription.setColumns(50);
        jTextAreaProjDescription.setLineWrap(true);
        jTextAreaProjDescription.setRows(14);
        jTextAreaProjDescription.setBorder(BorderFactory.createEmptyBorder(4,4,4,4));

        this.jTextAreaSimConfigDesc.setBorder(BorderFactory.createEtchedBorder());

        jPanelCellTypes.setLayout(borderLayout2);
        jButtonCellTypeNew.setText("New Cell Type");
        //jPanelCellTypeAddNew.setBorder(BorderFactory.createEtchedBorder());

       // jComboBox3DCellToView.addItem(defaultCellTypeToView);

        jButtonCellTypeNew.setEnabled(false);
        jButtonCellTypeNew.setText("Add New Cell Type to Project...");
        jButtonCellTypeNew.addActionListener(new java.awt.event.ActionListener()
        {
            public void actionPerformed(ActionEvent e)
            {
                jButtonCellTypeNew_actionPerformed(e);
            }
        });
        jButtonRegionNew.setEnabled(false);
        jButtonRegionNew.setText("Add New Region...");
        jButtonRegionNew.addActionListener(new java.awt.event.ActionListener()
        {
            public void actionPerformed(ActionEvent e)
            {
                jButtonAddRegion_actionPerformed(e);
            }
        });
        jPanelRegionsButtons.setBorder(BorderFactory.createEtchedBorder());
        jPanelRegionsButtons.setDebugGraphicsOptions(0);
        jPanelRegions.setLayout(borderLayout4);
        jPanelRegionsButtons.setBorder(BorderFactory.createEtchedBorder());
        jPanel3DDemo.setLayout(borderLayout5);
        jPanel3DButtons.setBorder(BorderFactory.createEtchedBorder());
        jPanel3DMain.setBorder(BorderFactory.createLoweredBevelBorder());
        jButton3DView.setEnabled(false);
        jButton3DView.setVerifyInputWhenFocusTarget(true);
        jButton3DView.setActionCommand("Construct 3D Demo of Network");
        jButton3DView.setText("View:");
        jButton3DView.addActionListener(new java.awt.event.ActionListener()
        {
            public void actionPerformed(ActionEvent e)
            {
                jButton3DView_actionPerformed(e);
            }
        });
        //jPanel1.setBorder(BorderFactory.createEtchedBorder());
        //jLabel1.setText("Details");
        jPanelCellTypeDetails.setLayout(borderLayout29);
        jPanelCellTypeDetails.setBorder(BorderFactory.createEtchedBorder());
        jPanelCellGroupDetails.setDebugGraphicsOptions(0);
        jPanelCellGroupDetails.setLayout(borderLayout6);
        jButtonNeuronRun.setEnabled(false);
        jButtonNeuronRun.setText("Run NEURON Simulation");
        jButtonNeuronRun.addActionListener(new java.awt.event.ActionListener()
        {
            public void actionPerformed(ActionEvent e)
            {
                jButtonNeuronRun_actionPerformed(e);
            }
        });
        jButtonNeuronCreateLocal.setEnabled(false);
        jButtonNeuronCreateLocal.setText("Create files for local execution");
        jButtonNeuronCreateLocal.addActionListener(new java.awt.event.ActionListener()
        {
            public void actionPerformed(ActionEvent e)
            {
                jButtonNeuronCreateLocal_actionPerformed(e);
            }
        });
        jPanelHocFileButtons.setEnabled(true);
        jPanelCellGroupsMainPanel.setBorder(BorderFactory.createEtchedBorder());
        jPanelCellGroupsMainPanel.setLayout(borderLayout7);
        jButton3DDestroy.setEnabled(false);
        jButton3DDestroy.setText("Stop 3D");
        jButton3DDestroy.addActionListener(new java.awt.event.ActionListener()
        {
            public void actionPerformed(ActionEvent e)
            {
                jButton3DDestroy_actionPerformed(e);
            }
        });
        /*
        jLabelWidth.setText("Width of 3D region:");
        jTextFieldWidth.setColumns(5);
        jLabelWidth.setText("Depth of 3D region:");
        jTextFieldDepth.setColumns(5);
        jLabelDepth.setText("Width of 3D region:");*/

 ///////////////////       jTable3DRegions.setModel(ProjectManager.getCurrentProject().regionsInfo);
        jPanelCellGroupButtons.setBorder(BorderFactory.createEtchedBorder());
        jButtonCellGroupsNew.setEnabled(false);
        jButtonCellGroupsNew.setText("New Cell Group...");
        jButtonCellGroupsNew.addActionListener(new java.awt.event.ActionListener()
        {
            public void actionPerformed(ActionEvent e)
            {
                jButtonCellGroupNew_actionPerformed(e);
            }
        });



        jPanelRegionsTable.setLayout(borderLayout8);
        jLabelName.setEnabled(false);
        jLabelName.setText("Project Name:");
        jLabelMainNumCells.setEnabled(false);
        jLabelMainNumCells.setText("Cell Types in project:");
        /*
        jTextFieldNumCells.setMinimumSize(new Dimension(240, 21));
        jTextFieldNumCells.setPreferredSize(new Dimension(240, 21));
        jTextFieldNumCells.setEditable(false);
        jTextFieldNumCells.setText("");
        jTextFieldNumCells.setColumns(30);*/


        jLabelProjDescription.setEnabled(false);
        jLabelProjDescription.setText("Project Description:");

        jTextFieldProjName.setEnabled(true);
        jTextFieldProjName.setMinimumSize(new Dimension(240, 21));
        jTextFieldProjName.setOpaque(true);
        jTextFieldProjName.setPreferredSize(new Dimension(389, 21));
        jTextFieldProjName.setEditable(false);
        jTextFieldProjName.setText("");
        jTextFieldProjName.setColumns(35);
        jPanelMainInfo.setLayout(gridBagLayout2);

        jPanelMainInfo.setBorder(BorderFactory.createEtchedBorder());
        jPanelMainInfo.setMaximumSize(new Dimension(800, 470));
        jPanelMainInfo.setMinimumSize(new Dimension(800, 470));
        jPanelMainInfo.setPreferredSize(new Dimension(800, 470));
        //jScrollPaneProjDesc.setVerticalScrollBarPolicy(JScrollPane.VERTICAL_SCROLLBAR_AS_NEEDED);

        jScrollPaneProjDesc.setMaximumSize(new Dimension(100, 100));
        jScrollPaneProjDesc.setMinimumSize(new Dimension(100, 100));
        jScrollPaneProjDesc.setPreferredSize(new Dimension(100, 100));
        jLabelSimConfigs.setEnabled(false);
        jLabelSimConfigs.setText("Simulation Configurations:");


        jMenuItemCopyProject.setEnabled(false);
        jPanelNetworkSettings.setLayout(borderLayout9);
        jMenuItemCloseProject.setEnabled(false);
        jMenuItemCloseProject.setText("Close Project");
        jMenuItemCloseProject.addActionListener(new java.awt.event.ActionListener()
        {
            public void actionPerformed(ActionEvent e)
            {
                jMenuItemCloseProject_actionPerformed(e);
            }
        });
        jLabelTitle.setEnabled(true);
        jLabelTitle.setFont(new java.awt.Font("Dialog", 1, 24));
        jLabelTitle.setForeground(new Color(50, 50, 50));
        jLabelTitle.setBorder(border1);
        jLabelTitle.setVerifyInputWhenFocusTarget(true);
        jLabelTitle.setHorizontalAlignment(SwingConstants.CENTER);
        jLabelTitle.setIcon(imageNeuroConstruct);
        jLabelTitle.setText("");
        jPanelExport.setLayout(borderLayout10);
        jLabelSimDefDur.setText("Default Simulation Duration: ");
        //jTextFieldDuration.setText("50");
        jTextFieldSimDefDur.setColumns(6);
        jTextFieldSimDefDur.setHorizontalAlignment(SwingConstants.RIGHT);
        jTextFieldSimDefDur.addKeyListener(new java.awt.event.KeyAdapter()
        {
            public void keyReleased(KeyEvent e)
            {
               //////////// jTextFieldDuration_keyReleased(e);
            }
        });

        //jPanelGenSimSettings.setBackground(Color.lightGray);
        jPanelSimSettings.setBorder(BorderFactory.createEtchedBorder());
        jPanelSimSettings.setLayout(borderLayout12);
        jLabelSimDT.setText("Simulation time step (dt):");
        //jTextFieldDT.setText("0.025");
        jTextFieldSimDT.setColumns(6);
        jTextFieldSimDT.setHorizontalAlignment(SwingConstants.RIGHT);
        jTextFieldSimDT.addKeyListener(new java.awt.event.KeyAdapter()
        {
            public void keyReleased(KeyEvent e)
            {
                jTextFieldDT_keyReleased(e);
            }
        });

        jLabelSimSummary.setText("Simulation Summary...");
        jButton3DSettings.setEnabled(false);
        jButton3DSettings.setText("3D Settings");
        jButton3DSettings.addActionListener(new java.awt.event.ActionListener()
        {
            public void actionPerformed(ActionEvent e)
            {
                jButton3DSettings_actionPerformed(e);
            }
        });
        jButtonNeuronView.setEnabled(false);
        jButtonNeuronView.setText("View:");
        jButtonNeuronView.addActionListener(new java.awt.event.ActionListener()
        {
            public void actionPerformed(ActionEvent e)
            {
                jButtonNeuronView_actionPerformed(e);
            }
        });
        jLabelSimRef.setText("Simulation Reference:");
        jTextFieldSimRef.setText("Sim_1");
        jTextFieldSimRef.setColumns(10);
        jRadioButtonNeuronSimSaveToFile.setText("Save to file");
        jPanelCellTypeInfo.setLayout(borderLayout30);
        jButtonNetSetAddNew.setEnabled(false);
        jButtonNetSetAddNew.setText("Add Morphology Connection");
        jButtonNetSetAddNew.addActionListener(new java.awt.event.ActionListener()
        {
            public void actionPerformed(ActionEvent e)
            {
                jButtonNetSetAddNew_actionPerformed(e);
            }
        });
        jButtonCellTypeViewCell.setEnabled(false);
        jButtonCellTypeViewCell.setText("View/edit morphology");
        jButtonCellTypeViewCell.addActionListener(new java.awt.event.ActionListener()
        {
            public void actionPerformed(ActionEvent e)
            {
                jButtonCellTypeViewCell_actionPerformed(e);
            }
        });

        jButtonCellTypeViewCellChans.setEnabled(false);
        jButtonCellTypeViewCellChans.setText("Edit membrane mechanisms");
        jButtonCellTypeViewCellChans.addActionListener(new java.awt.event.ActionListener()
        {
            public void actionPerformed(ActionEvent e)
            {
                jButtonCellTypeViewCellChans_actionPerformed(e);
            }
        });

        jLabelExistingCellTypes.setText("Cell Types included in project:  ");
        jComboBoxCellTypes.setEnabled(false);
        jComboBoxCellTypes.setMaximumSize(new Dimension(32767, 32767));
        jComboBoxCellTypes.setMinimumSize(new Dimension(240, 21));
        jComboBoxCellTypes.setPreferredSize(new Dimension(240, 21));
        jComboBoxCellTypes.setEditable(false);
        jComboBoxCellTypes.setMaximumRowCount(12);
        jComboBoxCellTypes.addItemListener(new java.awt.event.ItemListener()
        {
            public void itemStateChanged(ItemEvent e)
            {
                jComboBoxCellTypes_itemStateChanged(e);
            }
        });



        this.jComboBoxNeuroMLComps.addItemListener(new java.awt.event.ItemListener()
        {
            public void itemStateChanged(ItemEvent e)
            {
                jComboBoxNeuroMLComps_itemStateChanged(e);
            }
        });
        jTextAreaNeuroMLCompsDesc.setMinimumSize(new Dimension(700, 50));
        jTextAreaNeuroMLCompsDesc.setPreferredSize(new Dimension(700, 50));
        jTextAreaNeuroMLCompsDesc.setBorder(BorderFactory.createEtchedBorder());
        jTextAreaNeuroMLCompsDesc.setLineWrap(true);
        jTextAreaNeuroMLCompsDesc.setWrapStyleWord(true);
        jTextAreaNeuroMLCompsDesc.setEditable(false);


        this.jComboBoxGenesisComps.addItemListener(new java.awt.event.ItemListener()
        {
            public void itemStateChanged(ItemEvent e)
            {
                jComboBoxGenesisComps_itemStateChanged(e);
            }
        });
        jTextAreaGenesisCompsDesc.setMinimumSize(new Dimension(700, 50));
        jTextAreaGenesisCompsDesc.setPreferredSize(new Dimension(700, 50));
        jTextAreaGenesisCompsDesc.setBorder(BorderFactory.createEtchedBorder());
        jTextAreaGenesisCompsDesc.setLineWrap(true);
        jTextAreaGenesisCompsDesc.setWrapStyleWord(true);
        jTextAreaGenesisCompsDesc.setEditable(false);





        jComboBoxNeuronExtraBlocks.setEnabled(false);
        jComboBoxNeuronExtraBlocks.setEditable(false);
        jComboBoxNeuronExtraBlocks.addItemListener(new java.awt.event.ItemListener()
        {
            public void itemStateChanged(ItemEvent e)
            {
                jComboBoxNeuronExtraBlocks_itemStateChanged(e);
            }
        });
        jComboBoxNeuronExtraBlocks.addItem(this.neuronBlockPrompt);

        ArrayList<NativeCodeLocation> hocLocs = NativeCodeLocation.getAllKnownLocations();

        for (int i = 0; i < hocLocs.size(); i++)
        {
            jComboBoxNeuronExtraBlocks.addItem(hocLocs.get(i));
        }

        jComboBoxGenesisExtraBlocks.setEnabled(false);
        jComboBoxGenesisExtraBlocks.setEditable(false);
        jComboBoxGenesisExtraBlocks.addItemListener(new java.awt.event.ItemListener()
        {
            public void itemStateChanged(ItemEvent e)
            {
                jComboBoxGenesisExtraBlocks_itemStateChanged(e);
            }
        });
        jComboBoxGenesisExtraBlocks.addItem(this.genesisBlockPrompt);

        ArrayList<ScriptLocation> scriptlocs = ScriptLocation.getAllKnownLocations();

        for (int i = 0; i < scriptlocs.size(); i++)
        {
            jComboBoxGenesisExtraBlocks.addItem(scriptlocs.get(i));
        }





        jPanelGenerate.setLayout(borderLayout14);
        jComboBoxSimConfig.setEnabled(false);
        jButtonSimConfigEdit.setEnabled(false);
        jButtonGenerateSave.setEnabled(false);
        jButtonGenerateLoad.setEnabled(false);
        jCheckBoxGenerateZip.setEnabled(false);
        jCheckBoxGenerateExtraNetComments.setEnabled(false);
        jButtonGenerate.setEnabled(false);
        jButtonGenerate.setText("Generate Cell Positions and Connections");
        jButtonGenerateSave.setText("Save NetworkML");
        jButtonGenerateLoad.setText("Load NetworkML");

        jCheckBoxGenerateZip.setText("Compress");
        jCheckBoxGenerateExtraNetComments.setText("Extra comments");

        jButtonGenerateSave.addActionListener(new java.awt.event.ActionListener()
        {
            public void actionPerformed(ActionEvent e)
            {
                jButtonGenerateSave_actionPerformed(e);
            }
        });

        jButtonGenerateLoad.addActionListener(new java.awt.event.ActionListener()
        {
            public void actionPerformed(ActionEvent e)
            {
                jButtonGenerateLoad_actionPerformed(e);
            }
        });




        jButtonSimConfigEdit.setText("Edit Simulation Configs");

        jButtonGenerate.addActionListener(new java.awt.event.ActionListener()
        {
            public void actionPerformed(ActionEvent e)
            {
                jButtonGenerate_actionPerformed(e);
            }
        });
        this.jButtonSimConfigEdit.addActionListener(new java.awt.event.ActionListener()
        {
            public void actionPerformed(ActionEvent e)
            {
                jButtonSimConfigEdit_actionPerformed(e);
            }
        });


        scrollerGenerate.setVerticalScrollBarPolicy(JScrollPane.VERTICAL_SCROLLBAR_AS_NEEDED);

        scrollerGenerate.setMaximumSize(new Dimension(700, 260));
        scrollerGenerate.setMinimumSize(new Dimension(700, 260));
        scrollerGenerate.setPreferredSize(new Dimension(700, 260));
        jPanelGenerateLoadSave.setBorder(BorderFactory.createEtchedBorder());
        jPanelGenerateButtonsDesc.setBorder(BorderFactory.createEtchedBorder());
        jPanelGenerateMain.setBorder(BorderFactory.createEtchedBorder());
        jPanelGenerateMain.setLayout(gridBagLayout1);
        jButtonRegionRemove.setText("Delete selected Region");
        jButtonRegionRemove.addActionListener(new java.awt.event.ActionListener()
        {
            public void actionPerformed(ActionEvent e)
            {
                jButtonRegionRemove_actionPerformed(e);
            }
        });
        jButtonRegionRemove.setEnabled(false);
        jButtonCellGroupsDelete.setEnabled(false);
        jButtonCellGroupsDelete.setText("Delete selected Cell Group");
        jButtonCellGroupsDelete.addActionListener(new java.awt.event.ActionListener()
        {
            public void actionPerformed(ActionEvent e)
            {
                jButtonCellGroupDelete_actionPerformed(e);
            }
        });
        //jPanelNetSetTable.setLayout(borderLayout15);
        jButtonNetConnDelete.setEnabled(false);
        jButtonNetConnDelete.setText("Delete selected Morph Conn");
        jButtonNetConnDelete.addActionListener(new java.awt.event.ActionListener()
        {
            public void actionPerformed(ActionEvent e)
            {
                jButtonNetConnDelete_actionPerformed(e);
            }
        });
        jRadioButtonNeuronSimDontRecord.setText("Don\'t save");

        jPanelNmodl.setLayout(gridLayout1);
        jPanelSynapseButtons.setBorder(BorderFactory.createEtchedBorder());
        jPanelSynapseMain.setBorder(BorderFactory.createEtchedBorder());
        jPanelSynapseMain.setLayout(borderLayout16);
        /*
        jButtonSynapseAdd.setText("Add New Custom Synapse Type");
        jButtonSynapseAdd.addActionListener(new java.awt.event.ActionListener()
        {
            public void actionPerformed(ActionEvent e)
            {
                jButtonSynapseAdd_actionPerformed(e);
            }
        });*/
        jPanelStims.setLayout(borderLayout17);
        jLabelGenAnalyse.setText("Analyse:");
        jPanelGenerateAnalyse.setMaximumSize(new Dimension(200, 40));
        jPanelGenerateAnalyse.setMinimumSize(new Dimension(200, 40));
        jPanelGenerateAnalyse.setPreferredSize(new Dimension(200, 40));
        jComboBoxAnalyseCellGroup.setEnabled(false);
        jComboBoxAnalyseNetConn.setEditable(false);


/*
        jButtonSynapseEdit.setText("Edit Selected Synapse Type");
        jButtonSynapseEdit.addActionListener(new java.awt.event.ActionListener()
        {
            public void actionPerformed(ActionEvent e)
            {
                jButtonSynapseEdit_actionPerformed(e);
            }
        });*/
        jMenuItemGeneralProps.setText("General Properties & Project Defaults");
        jMenuItemGeneralProps.addActionListener(new java.awt.event.ActionListener()
        {
            public void actionPerformed(ActionEvent e)
            {
                jMenuItemGeneralProps_actionPerformed(e);
            }
        });

        jButtonGenerateStop.setEnabled(false);
        jTextAreaSimConfigDesc.setEditable(false);
        jTextAreaSimConfigDesc.setEnabled(false);
        jButtonGenerateStop.setText("Stop Generation");
        jButtonGenerateStop.addActionListener(new java.awt.event.ActionListener()
        {
            public void actionPerformed(ActionEvent e)
            {
                jButtonGenerateStop_actionPerformed(e);
            }
        });
        jButtonNetConnEdit.setEnabled(false);
        jButtonNetConnEdit.setText("Edit selected Morph Conn");
        jButtonNetConnEdit.addActionListener(new java.awt.event.ActionListener()
        {
            public void actionPerformed(ActionEvent e)
            {
                jButtonNetConnEdit_actionPerformed(e);
            }
        });
        jButtonAnalyseConnLengths.setEnabled(false);
        jButtonAnalyseConnLengths.setText("Analyse connection lengths");
        jButtonAnalyseConnLengths.addActionListener(new java.awt.event.ActionListener()
        {
            public void actionPerformed(ActionEvent e)
            {
                jButtonAnalyseConns_actionPerformed(e);
            }
        });
        jComboBoxAnalyseNetConn.setEnabled(false);


        jComboBoxAnalyseNetConn.addActionListener(new ActionListener()
        {
            public void actionPerformed(ActionEvent e)
            {
                //System.out.println("e: " + e);
                //if (e.getID()== ItemEvent.SELECTED)
                //{
                    jComboBoxAnalyseCellGroup.removeAllItems();
                    String sel = (String) jComboBoxAnalyseNetConn.getSelectedItem();
                    String selCellGroup = (String) jComboBoxAnalyseCellGroup.getSelectedItem();

                    jComboBoxAnalyseCellGroup.addItem(defaultAnalyseCellGroupString);

                    if (projManager.getCurrentProject() != null)
                    {
                        if (projManager.getCurrentProject().volBasedConnsInfo.isValidAAConn(sel))
                        {
                            jComboBoxAnalyseCellGroup.addItem(projManager.getCurrentProject().volBasedConnsInfo.
                                                              getSourceCellGroup(sel));
                            jComboBoxAnalyseCellGroup.addItem(projManager.getCurrentProject().volBasedConnsInfo.
                                                              getTargetCellGroup(sel));
                        }
                        if (projManager.getCurrentProject().morphNetworkConnectionsInfo.isValidSimpleNetConn(sel))
                        {
                            jComboBoxAnalyseCellGroup.addItem(projManager.getCurrentProject().
                                                              morphNetworkConnectionsInfo.
                                                              getSourceCellGroup(sel));
                            jComboBoxAnalyseCellGroup.addItem(projManager.getCurrentProject().
                                                              morphNetworkConnectionsInfo.
                                                              getTargetCellGroup(sel));
                        }

                        if (jComboBoxAnalyseCellGroup.getItemCount() > 1)
                        {
                            jComboBoxAnalyseCellGroup.setSelectedIndex(1);
                        }
                        if (selCellGroup!=null && !selCellGroup.equals(defaultAnalyseCellGroupString))
                        {
                            jComboBoxAnalyseCellGroup.setSelectedItem(selCellGroup); // Will do nothing if not in list...
                        }
                    }
                //}
            }
            });

            jButtonAnalyseNumConns.setEnabled(false);
            jButtonAnalyseNumConns.setText("Analyse number of connections");
            jButtonAnalyseNumConns.addActionListener(new java.awt.event.ActionListener()
            {
                public void actionPerformed(ActionEvent e)
                {
                    jButtonAnalyseNumConns_actionPerformed(e);
                }
            });

            this.jButtonAnalyseCellDensities.setEnabled(false);
            jButtonAnalyseCellDensities.setText("Analyse cell densities");
            jButtonAnalyseCellDensities.addActionListener(new java.awt.event.ActionListener()
            {
                public void actionPerformed(ActionEvent e)
                {
                    jButtonAnalyseCellDensities_actionPerformed(e);
                }
            });



        jPanelRegions.add(jPanelRegionsTable, BorderLayout.CENTER);
        jPanelRegions.add(jPanelRegionsButtons, BorderLayout.NORTH);
        jPanelRegionsButtons.add(jButtonRegionNew, null);
        jPanelRegionsButtons.add(jButtonRegionsEdit, null);
        jToolBar.add(jButtonNewProject, null);
        jToolBar.add(jButtonOpenProject);
        jToolBar.add(jButtonSaveProject);
        jToolBar.add(jButtonCloseProject);
        jToolBar.add(jButtonPreferences);
        jToolBar.addSeparator();
        jToolBar.addSeparator();
        jToolBar.add(jButtonToggleTips);
        jToolBar.add(jButtonToggleConsoleOut);
        jToolBar.addSeparator();
        jToolBar.addSeparator();
        jToolBar.add(jButtonValidate);


        jMenuFile.add(jMenuItemNewProject);
        jMenuFile.add(jMenuItemFileOpen);

        jMenuFile.add(jMenuItemSaveProject);


        jMenuFile.add(jMenuItemCloseProject);
        jMenuFile.addSeparator();
        jMenuFile.add(jMenuItemCopyProject);

        jMenuFile.add(jMenuItemZipUp);
        jMenuFile.add(jMenuItemUnzipProject);

        jMenuFile.addSeparator();
        jMenuFile.add(jMenuFileExit);

        jMenuHelp.add(jMenuItemHelp);
        jMenuHelp.add(jMenuItemGlossary);
        jMenuHelp.add(jMenuItemUnits);
        jMenuHelp.add(jMenuItemJava);
        jMenuHelp.add(jMenuHelpAbout);

        jMenuBar1.add(jMenuFile);
        jMenuBar1.add(jMenuProject);
        jMenuBar1.add(jMenuSettings);
        jMenuBar1.add(jMenuTools);
        jMenuBar1.add(jMenuHelp);

        this.setJMenuBar(jMenuBar1);
        contentPane.add(jToolBar, BorderLayout.NORTH);
        contentPane.add(statusBar, BorderLayout.SOUTH);
        contentPane.add(jTabbedPaneMain, BorderLayout.CENTER);
        jMenuSettings.add(jMenuItemProjProperties);
        jMenuSettings.add(jMenuItemGeneralProps);


        //jMenuTools.add(jMenuItemNmodlEditor);
        jMenuTools.add(jMenuItemViewProjSource);
        jMenuTools.addSeparator();
        jMenuTools.add(jMenuItemCondorMonitor);
        jMenuTools.add(jMenuItemMPIMonitor);
        jMenuTools.addSeparator();
        jMenuTools.add(jMenuItemPlotImport);
        jMenuTools.add(jMenuItemPlotEquation);





        jPanelSynapseButtons.add(jLabelSynapseTitle,  BorderLayout.NORTH);
        jPanelSynapseButtons.add(jPanelSynapseButtonsOnly, BorderLayout.CENTER);
        jPanelSynapseButtonsOnly.add(jButtonSynapseEdit, null);
        jPanelSynapseMain.add(jScrollPaneSynapses, BorderLayout.CENTER);
        jPanelChannelMechsMain.add(jScrollPaneChanMechs, BorderLayout.CENTER);
        jPanelNmodl.add(jPanelChannelMechsInnerTab, null);
        jScrollPaneSynapses.getViewport().add(jTableSynapses, null);
        jScrollPaneChanMechs.getViewport().add(jTableChanMechs, null);
        /*
        jPanelSynapticProcesses.add(jPanelSynapseMain, BorderLayout.CENTER);
        jPanelNmodl.add(jPanelSynapticProcesses, null);
        jPanelSynapticProcesses.add(jPanelSynapseButtons,  BorderLayout.NORTH);*/

        jPanelCellTypesButtonsInfo.add(jButtonCellTypeViewCellInfo, null);
        jPanelCellTypesButtonsInfo.add(jButtonCellTypeEditDesc, null);
        jPanelCellTypesButtonsInfo.add(jButtonCellTypeBioPhys, null);
        jPanelCellTypesButtonsInfo.add(jButtonCellTypeViewCell, null);
        jPanelCellTypesButtonsInfo.add(jButtonCellTypeViewCellChans, null);

        jPanelCellTypeInfo.add(jPanelCellTypesComboBox, BorderLayout.NORTH);
        jPanelCellTypesComboBox.add(jLabelExistingCellTypes, null);
        jPanelCellTypesComboBox.add(jComboBoxCellTypes, null);
        jPanelCellTypes.add(jPanelCellTypeDetails, BorderLayout.CENTER);
        jPanelCellTypeDetails.add(jPanelCellTypesAddNewCell,  BorderLayout.NORTH);
        jPanelCellTypesAddNewCell.add(jButtonCellTypeNew, null);
        jPanelCellTypesAddNewCell.add(jButtonCellTypeOtherProject, null);
        jPanelCellTypeDetails.add(jPanelCellTypeInfo, BorderLayout.CENTER);
        //jPanelCellTypeTree.add(jTreeCellDetails, null);

        //jPanelCellTypes.add(jPanel2,  BorderLayout.WEST);

        jPanelGenerate.add(jPanelGenerateButtonsDesc, BorderLayout.NORTH);
        jPanelGenerate.add(jPanelGenerateLoadSave, BorderLayout.SOUTH);

        jPanelGenerateButtons.add(jButtonGenerate, null);
        jPanelGenerateButtons.add(jComboBoxSimConfig, null);

        jPanelGenerateLoadSave.add(jButtonGenerateSave);
        jPanelGenerateLoadSave.add(jCheckBoxGenerateZip);
        jPanelGenerateLoadSave.add(this.jCheckBoxGenerateExtraNetComments);
        jPanelGenerateLoadSave.add(jButtonGenerateLoad);

        jComboBoxSimConfig.addItemListener(new java.awt.event.ItemListener()
        {
            public void itemStateChanged(ItemEvent e)
            {
                jComboBoxSimConfig_itemStateChanged(e);
            }
        });


        jPanelGenerateButtons.add(jButtonSimConfigEdit, null);
        jPanelGenerateButtons.add(jButtonGenerateStop, null);

        jPanelGenerateDesc.add(jTextAreaSimConfigDesc, null);

        jPanelGenerateButtonsDesc.setLayout(new BorderLayout());

        ///jPanelGenerateLoadSave.setLayout(new BorderLayout());


        jPanelRandomGen.add(jLabelRandomGenDesc);
        jPanelRandomGen.add(jTextFieldRandomGen);
        jTextFieldRandomGen.setColumns(12);
        jTextFieldRandomGen.setText("12345");
        jCheckBoxRandomGen.setSelected(true);
        jPanelRandomGen.add(jCheckBoxRandomGen);

        jPanelNeuronNumInt.add(this.jCheckBoxNeuronNumInt);


        jPanelNeuronRandomGen.add(jLabelNeuronRandomGenDesc, BorderLayout.WEST);
        //jPanelNeuronRandomGen.setPreferredSize(new Dimension(700,62));
        jPanelNeuronRandomGen.setLayout(new FlowLayout());
        jPanelNeuronRandomGen.setBorder(BorderFactory.createEmptyBorder());
        Dimension dim = new Dimension(600,50);
        jPanelNeuronRandomGen.setPreferredSize(dim);
        jPanelNeuronRandomGen.setMinimumSize(dim);
        jPanelNeuronRandomGen.add(jTextFieldNeuronRandomGen, BorderLayout.CENTER);

        jTextFieldNeuronRandomGen.setColumns(12);
        jTextFieldNeuronRandomGen.setText("12345");
        jCheckBoxNeuronRandomGen.setSelected(true);
        jPanelNeuronRandomGen.add(jCheckBoxNeuronRandomGen, BorderLayout.EAST);

        BorderLayout blComp = new BorderLayout();
        blComp.setHgap(12);
        blComp.setVgap(12);
        this.jPanelGenesisComps.setLayout(blComp);
        this.jPanelGenesisComps.add(jLabelGenesisCompsDesc, BorderLayout.WEST);
        this.jPanelGenesisComps.add(this.jComboBoxGenesisComps, BorderLayout.CENTER);
        this.jPanelGenesisComps.add(this.jTextAreaGenesisCompsDesc, BorderLayout.SOUTH);




        jPanelGenesisRandomGen.add(jLabelGenesisRandomGenDesc);
        jPanelGenesisRandomGen.add(jTextFieldGenesisRandomGen);
        jTextFieldGenesisRandomGen.setColumns(12);
        jTextFieldGenesisRandomGen.setText("12345");
        jCheckBoxGenesisRandomGen.setSelected(true);
        jPanelGenesisRandomGen.add(jCheckBoxGenesisRandomGen);



        jPanelGenerateButtonsDesc.add(jPanelGenerateButtons, BorderLayout.NORTH);
        jPanelGenerateButtonsDesc.add(jPanelGenerateDesc, BorderLayout.CENTER);
        jPanelGenerateButtonsDesc.add(jPanelRandomGen, BorderLayout.SOUTH);

        jTextAreaSimConfigDesc.setSize(600,100);
        jTextAreaSimConfigDesc.setRows(3);
        jTextAreaSimConfigDesc.setWrapStyleWord(true);
        jTextAreaSimConfigDesc.setLineWrap(true);

        jPanelGenerate.add(jPanelGenerateMain, BorderLayout.CENTER);

        jPanelExport.add(jTabbedPaneExportFormats, BorderLayout.CENTER);
        jPanelInputOutput.add(jPanelSimPlot,  BorderLayout.SOUTH);
        jPanelSimPlot.add(jScrollPaneSimPlot, BorderLayout.CENTER);
        jScrollPaneSimPlot.getViewport().add(jTableSimPlot, null);
        jScrollPaneSimStims.getViewport().add(jTableStims, null);

        jPanelSimPlot.add(jPanelSimPlotButtons, BorderLayout.NORTH);
        jPanelSimPlotButtons.add(jButtonSimPlotAdd, null);
        jPanelSimPlotButtons.add(jButtonSimPlotEdit, null);

        jPanelInputOutput.add(jPanelStims, BorderLayout.CENTER);

        jLabelSimStimDesc.setText("The following stimulations are applied to the network");

        //jPanelSimulationChoices.add(jLabelSimStimDesc,  BorderLayout.NORTH);
        jPanelStims.add(jPanelSimStimButtons,  BorderLayout.NORTH);

       // jPanelSimRecord.add(jPanelSimWhatToRecord, BorderLayout.NORTH);

        jPanelGenesisSettings.add(jLabelGenesisMain,  BorderLayout.NORTH);

        jPanelGenesisMain.setLayout(this.gridBagLayoutGen);

        jPanelGenesisMain.add(jPanelGenesisSettings,
                              new GridBagConstraints(0, 0, 1, 1, 0.0, 0.0
                                                     ,GridBagConstraints.CENTER,
                                                     GridBagConstraints.NONE,
                                                     new Insets(20, 0, 20, 0), 20, 20));

        jPanelGenesisMain.add(this.jPanelGenesisRandomGen,
                              new GridBagConstraints(0, 1, 1, 1, 0.0, 0.0
                                                     , GridBagConstraints.CENTER,
                                                     GridBagConstraints.NONE,
                                                     new Insets(0, 0, 20, 0), 0, 0));

        jPanelGenesisMain.add(this.jPanelGenesisComps,
                              new GridBagConstraints(0, 2, 1, 1, 0.0, 0.0
                                                     , GridBagConstraints.CENTER,
                                                     GridBagConstraints.NONE,
                                                     new Insets(0, 0, 0, 0), 20, 0));

        jPanelGenesisMain.add(jPanelGenesisButtons,
                              new GridBagConstraints(0, 3, 1, 1, 0.0, 0.0
                                                     , GridBagConstraints.CENTER,
                                                     GridBagConstraints.NONE,
                                                     new Insets(0, 0, 200, 0), 0, 0));



        //jTabbedPaneExportFormats.addTab(NEOSIM_SIMULATOR_TAB, null, jPanelExportNeosim, toolTipText.getToolTip("NEOSIM"));

        jPanelExportNeuron.setLayout(gridLayout5);


        jPanelHocFileButtons.add(jButtonNeuronCreateLocal, null);

        //jPanelHocFileButtons.add(jButto nNeuronCreateCondor, null);

        if (includeParallelFunc()) jPanelHocFileButtons.add(jButtonNeuronCreateMPI, null);

        jPanelHocFileButtons.add(jButtonNeuronView, null);
        jPanelHocFileButtons.add(jComboBoxNeuronFiles, null);
        jPanelHocFileButtons.add(jButtonNeuronRun, null);

        jPanelNeuronMainSettings.add(jLabelNeuronMainLabel,  new GridBagConstraints(0, 0, 1, 1, 0.0, 0.0
            ,GridBagConstraints.CENTER, GridBagConstraints.NONE, new Insets(0, 0, 0, 0), 673, 20));


        jPanelNeuronMainSettings.add(jPanelNeuronGraphOptions,    new GridBagConstraints(0, 1, 1, 1, 1.0, 1.0
            ,GridBagConstraints.CENTER, GridBagConstraints.NONE, new Insets(0, 0, 0, 0), 0, 20));


        jPanelNeuronMainSettings.add(jPanelNeuronNumInt,    new GridBagConstraints(0, 2, 1, 1, 1.0, 1.0
            ,GridBagConstraints.CENTER, GridBagConstraints.NONE, new Insets(0, 0, 0, 0), 0, 20));

        jPanelNeuronMainSettings.add(this.jPanelNeuronRandomGen,    new GridBagConstraints(0, 3, 1, 1, 1.0, 1.0
            ,GridBagConstraints.CENTER, GridBagConstraints.NONE, new Insets(0, 0, 0, 0), 0, 0));


        jPanelNeuronMainSettings.add(jPanelHocFileButtons,        new GridBagConstraints(0, 4, 1, 1, 1.0, 1.0
            ,GridBagConstraints.CENTER, GridBagConstraints.BOTH, new Insets(0, 0, 250, 0), 0, 70));


  /*      jPanelNeuronMainSettings.add(jLabelNeuronMainLabel);

        jPanelNeuronMainSettings.add(jPanelNeuronGraphOptions);

        jPanelNeuronMainSettings.add(this.jPanelNeuronRandomGen);

        jPanelNeuronMainSettings.add(jPanelHocFileButtons);  */



        jPanelSimGeneral.add(jPanelSimSettings, BorderLayout.NORTH);
        jPanelExportNeuron.add(jTabbedPaneNeuron, null); //jTabbedPaneNeuron.addTab("NMODL mechanisms", null, jPanelNmodl, toolTipText.getToolTip("NMODL"));




        jPanelCellGroupDetails.add(jPanelCellGroupsMainPanel, BorderLayout.CENTER);
        jPanelCellGroupsMainPanel.add(jScrollPaneCellGroups, BorderLayout.CENTER);

        jScrollPaneCellGroups.getViewport().add(jTableCellGroups, null);

        jPanelCellGroupDetails.add(jPanelCellGroupButtons, BorderLayout.NORTH);
        jPanelCellGroupButtons.add(jButtonCellGroupsNew, null);
        jPanelCellGroupButtons.add(jButtonCellGroupsEdit, null);
        jPanelCellGroupButtons.add(jButtonCellGroupsDelete, null);

        jPanelProjInfo.add(jLabelTitle, new GridBagConstraints(0, 0, 1, 1, 0.0, 0.0
            , GridBagConstraints.CENTER, GridBagConstraints.NONE, new Insets(25, 0, 0, 0), 0, 0));

        jPanelProjInfo.add(jPanelMainInfo,    new GridBagConstraints(0, 1, 1, 1, 1.0, 1.0
            ,GridBagConstraints.CENTER, GridBagConstraints.NONE, new Insets(0, 0, 0, 0), 0, 34));

        jPanelMainInfo.add(jLabelName,     new GridBagConstraints(0, 0, 1, 1, 0.0, 0.0
            ,GridBagConstraints.WEST, GridBagConstraints.NONE, new Insets(6, 20, 6, 0), 0, 0));
        jPanelMainInfo.add(jTextFieldProjName,      new GridBagConstraints(2, 0, 1, 1, 1.0, 0.0
            ,GridBagConstraints.WEST, GridBagConstraints.HORIZONTAL, new Insets(6, 0, 6, 20), 0, 0));
        jPanelMainInfo.add(jLabelProjDescription,    new GridBagConstraints(0, 1, 1, 1, 0.0, 0.0
            ,GridBagConstraints.WEST, GridBagConstraints.NONE, new Insets(0, 20, 0, 0), 77, 70));

        //jPanelMainInfo.add(jTextAreaProjDescription,    new GridBagConstraints(1, 1, 1, 1, 1.0, 1.0
        //   ,GridBagConstraints.WEST, GridBagConstraints.NONE, new Insets(12, 0, 12, 0), 0, 0));

        jPanelMainInfo.add(jScrollPaneProjDesc,              new GridBagConstraints(2, 1, 1, 1, 0.0, 0.0
            ,GridBagConstraints.WEST, GridBagConstraints.NONE, new Insets(6, 0, 6, 0), 438,100));




        jPanelMainInfo.add(jLabelMainNumCells, new GridBagConstraints(0, 2, 1, 1, 0.0, 0.0
                                                                      , GridBagConstraints.WEST, GridBagConstraints.NONE,
                                                                      new Insets(6, 20, 6, 0), 0, 0));

        jPanelMainInfo.add(jLabelNumCellGroups, new GridBagConstraints(0, 3, 1, 1, 0.0, 0.0
                                                                       , GridBagConstraints.WEST, GridBagConstraints.NONE,
                                                                       new Insets(6, 20, 6, 0), 0, 0));

        jPanelMainInfo.add(jLabelSimConfigs, new GridBagConstraints(0, 4, 1, 1, 0.0, 0.0
                                                                    , GridBagConstraints.WEST, GridBagConstraints.NONE,
                                                                    new Insets(6, 20, 6, 0), 0, 0));



        jPanelCellClicks.setBorder(BorderFactory.createEtchedBorder());

        jPanelMainInfo.add(this.jPanelCellClicks, new GridBagConstraints(2, 2, 1, 1, 0.0, 0.0
                                                                      , GridBagConstraints.WEST,
                                                                      GridBagConstraints.HORIZONTAL, new Insets(6, 0, 6, 20),
                                                                      0, 0));

        jPanelCellGroupClicks.setBorder(BorderFactory.createEtchedBorder());

        jPanelMainInfo.add(jPanelCellGroupClicks, new GridBagConstraints(2, 3, 1, 1, 1.0, 0.0
                                                                        , GridBagConstraints.WEST,
                                                                        GridBagConstraints.HORIZONTAL,
                                                                        new Insets(6, 0, 6, 20), 0, 0));


        this.jPanelSimConfigClicks.setBorder(BorderFactory.createEtchedBorder());
        jPanelMainInfo.add(jPanelSimConfigClicks, new GridBagConstraints(2, 4, 1, 1, 0.0, 0.0
                                                                         , GridBagConstraints.CENTER,
                                                                         GridBagConstraints.HORIZONTAL,
                                                                         new Insets(6, 0, 6, 20), 0, 0));



        //jScrollPaneProjDesc.setViewportView(jTextAreaProjDescription);

        //jPanel1.add(jComboBox1, null);
        //jPanel1.add(jButton1, null);
        jPanel3DDemo.add(jPanel3DButtons, BorderLayout.NORTH);
    //    jPanel3DButtons.add(jComboBox3DCellToView, null);
        jPanel3DButtons.add(jButton3DView, null);
        jPanel3DButtons.add(jComboBoxView3DChoice, null);
        jPanel3DButtons.add(jButton3DDestroy, null);
        jPanel3DButtons.add(jButton3DSettings, null);

        jPanel3DButtons.add(new JLabel("         "), null);

        jPanel3DButtons.add(jButton3DPrevSims, null);


        jPanel3DDemo.add(jPanel3DMain, BorderLayout.CENTER);
/*
        jPanelRegionsButtons.add(jLabelWidth, null);
        jPanelRegionsButtons.add(jTextFieldWidth, null);
        jPanelRegionsButtons.add(jLabelDepth, null);
        jPanelRegionsButtons.add(jTextFieldDepth, null);
      */
        jPanelRegionsButtons.add(jButtonRegionRemove, null);
        jPanelRegionsTable.add(jScrollPaneRegions, BorderLayout.CENTER);
        jScrollPaneRegions.getViewport().add(jTable3DRegions, null);
        /*
         jPanelMainInfo.add(jTextFieldNumCells,  new GridBagConstraints(2, 3, 1, 1, 0.0, 0.0
         ,GridBagConstraints.WEST, GridBagConstraints.HORIZONTAL, new Insets(12, 0, 12, 20), 0, 0));
         */
        //jPanelSimMain.add(jLabel1,      new GridBagConstraints(1, 3, 1, 1, 0.0, 0.0
        //   ,GridBagConstraints.WEST, GridBagConstraints.NONE, new Insets(12, 20, 12, 0), 0, 0));


        jPanelSimSettings.add(jPanelSimValsInput, BorderLayout.CENTER);
        jPanelSimSettings.add(jPanelSimStorage, BorderLayout.SOUTH);

        jPanelSimValsInput.add(jLabelSimSummary,  BorderLayout.SOUTH);
        jPanelSimValsInput.add(jPanelSimTotalTime, BorderLayout.NORTH);
        jPanelSimTotalTime.add(jLabelSimDefDur, null);
        jPanelSimTotalTime.add(jTextFieldSimDefDur, null);
        jPanelSimTotalTime.add(jTextFieldSimTotalTimeUnits, null);
        jPanelSimDT.add(jLabelSimDT, null);
        jPanelSimDT.add(jTextFieldSimDT, null);
        jPanelSimDT.add(jTextFieldSimDTUnits, null);

        jTextFieldSimTotalTimeUnits.setText(
             UnitConverter.timeUnits[UnitConverter.NEUROCONSTRUCT_UNITS].getSymbol());
         jTextFieldSimDTUnits.setText(
                      UnitConverter.timeUnits[UnitConverter.NEUROCONSTRUCT_UNITS].getSymbol());

        //jPanelSimStorage.add(jRadioButtonNeuronSimDontRecord, null);
        //jPanelSimStorage.add(jRadioButtonNeuronSimSaveToFile, null);
        jPanelSimStorage.add(jLabelSimRef, null);
        jPanelSimStorage.add(jTextFieldSimRef, null);
        jPanelSimStorage.add(jCheckBoxSpecifySimRef, null);


        buttonGroupSimSavePreference.add(jRadioButtonNeuronSimSaveToFile);
        buttonGroupSimSavePreference.add(jRadioButtonNeuronSimDontRecord);

        jPanelNetworkSettings.add(jPanelAllNetSettings, BorderLayout.CENTER);



        jPanelCellTypeMainInfo.add(scrollerCellTypeInfo, null);

        jPanelCellTypeInfo.add(jPanelCellTypeMainButtons, BorderLayout.SOUTH);

        jPanelCellTypeInfo.add(jPanelCellTypeMainInfo, BorderLayout.CENTER);

        jComboBoxCellTypes.addItem(cellComboPrompt);

        JViewport vpCellType = scrollerCellTypeInfo.getViewport();
        vpCellType.add(jEditorPaneCellTypeInfo);

        JViewport vpGenerate = scrollerGenerate.getViewport();

        vpGenerate.add(
            jEditorPaneGenerateInfo);

        jPanelGenerateMain.add(jPanelGenerateAnalyse,
                               new GridBagConstraints(0, 2, 1, 1,
                                                      1.0, 1.0
                                                      ,GridBagConstraints.CENTER,
                                                      GridBagConstraints.BOTH,
                                                      new Insets(0, 0, 0, 0), 0,0));


        jPanelSimGeneral.add(jPanelSimulationParams,  BorderLayout.CENTER);
        //jPanelSimGeneral.add(this.jPanelSimWhatToRecord,  BorderLayout.SOUTH);
        jEditorPaneGenerateInfo.setContentType("text/html");
        jEditorPaneCellTypeInfo.setContentType("text/html");
        jEditorPaneCellTypeInfo.setEditable(false);

        jEditorPaneCellTypeInfo.setMinimumSize(new Dimension(700,400));
        jEditorPaneCellTypeInfo.setPreferredSize(new Dimension(700,400));


        scrollerCellTypeInfo.setMinimumSize(new Dimension(700, 400));
        scrollerCellTypeInfo.setPreferredSize(new Dimension(700,400));
        scrollerCellTypeInfo.setVerticalScrollBarPolicy(JScrollPane.VERTICAL_SCROLLBAR_ALWAYS);


        jEditorPaneGenerateInfo.setEditable(false);
        jPanelGenerateComboBoxes.add(jLabelGenAnalyse, null);
        jPanelGenerateComboBoxes.add(jComboBoxAnalyseNetConn, null);
        jPanelGenerateComboBoxes.add(jComboBoxAnalyseCellGroup, null);

        jPanelGenerateMain.add(scrollerGenerate,  new GridBagConstraints(0, 1, 1, 1, 1.0, 1.0
            ,GridBagConstraints.CENTER, GridBagConstraints.NONE, new Insets(0, 0, 0, 0), 0, 0));

        jPanelGenerateMain.add(jProgressBarGenerate,    new GridBagConstraints(0, 0, 1, 1, 0.0, 0.0
            ,GridBagConstraints.CENTER, GridBagConstraints.NONE, new Insets(12, 0, 6, 0), 400, 0));
    /*
        jPanelChannelMechsInnerTab.add(jPanelChanMechsButtons,  BorderLayout.NORTH);
        jPanelChanMechsButtons.add(jLabelChanMechTitle,  BorderLayout.NORTH);
        jPanelChanMechsButtons.add(jPanelChanMechsButtonsOnly,  BorderLayout.CENTER);

        jPanelChannelMechsInnerTab.add(jPanelChannelMechsMain, BorderLayout.CENTER);*/




        jPanelMainInfo.add(jLabelProjFileVersion,     new GridBagConstraints(0, 5, 1, 1, 0.0, 0.0
            ,GridBagConstraints.WEST, GridBagConstraints.NONE, new Insets(6, 20, 6, 0), 0, 0));
        jPanelMainInfo.add(jTextFieldProjFileVersion,       new GridBagConstraints(2, 5, 1, 1, 0.0, 0.0
            ,GridBagConstraints.WEST, GridBagConstraints.HORIZONTAL, new Insets(6, 0, 6, 20), 0, 0));
        jPanelMainInfo.add(jLabelMainLastModified,  new GridBagConstraints(0, 6, 1, 1, 0.0, 0.0
            ,GridBagConstraints.WEST, GridBagConstraints.NONE, new Insets(6, 20, 0, 0), 0, 0));
        jPanelNetSetAA.add(jPanelNetSetAAControls,  BorderLayout.NORTH);
        jPanelNetSetAAControls.add(jLabelNetSetAA,  BorderLayout.NORTH);
        jPanelNetSetAA.add(jPanelNetSetAATable, BorderLayout.CENTER);


        jPanelSimNeosimMain.add(jLabelSimulatorNeosimMain, null);
        jPanelNeuroML.add(jPanelNeuroMLHeader,  BorderLayout.NORTH);
        jPanelNeuroMLHeader.add(jLabelNeuroMLMain, null);
        jPanelNeuroML.add(jPanelNeuroMLButtons, BorderLayout.SOUTH);

        jPanelNeuroML.add(jPanelNeuroMLView,  BorderLayout.CENTER);
        jPanelExport.add(jPanelExportHeader, BorderLayout.NORTH);
        jPanelExportHeader.add( jLabelExportMain, null);

        jPanelCellTypeManageNumbers.add(jButtonCellTypeDelete, null);
        jPanelCellTypeManageNumbers.add(jButtonCellTypeCompare, null);

        jPanelCellTypeManageNumbers.add(jButtonCellTypeCopy, null);
        jPanelMainInfo.add(jTextFieldMainLastModified,   new GridBagConstraints(2, 6, 1, 1, 0.0, 0.0
            ,GridBagConstraints.CENTER, GridBagConstraints.HORIZONTAL, new Insets(6, 0, 0, 20), 0, 0));
        jPanelGenesisButtons.add(jButtonGenesisGenerate, null);
        jPanelGenesisButtons.add(jButtonGenesisView, null);
        jPanelGenesisButtons.add(jComboBoxGenesisFiles, null);
        jPanelGenesisButtons.add(jButtonGenesisRun, null);
        jPanelExportGenesis.add(jTabbedPaneGenesis,  BorderLayout.CENTER);
        jTabbedPaneGenesis.add(jPanelGenesisMain,   "Generate code");
        jPanelNeuronExtraHoc.add(jPanelNeuronExtraHocBlock, null);
        jPanelGenesisExtra.add(jPanelGenesisExtraBlock, null);



        jPanelNeuronBlockDesc.add(this.jTextAreaNeuronBlockDesc);
        jPanelGenesisBlockDesc.add(this.jTextAreaGenesisBlockDesc);

        jPanelCBNeuronExtraBlocks.add(this.jComboBoxNeuronExtraBlocks);
        jPanelCBGenesisExtraBlocks.add(this.jComboBoxGenesisExtraBlocks);

        jPanelNeuronExtraHocBlock.add(jPanelCBNeuronExtraBlocks, BorderLayout.NORTH);
        jPanelNeuronExtraHocBlock.add(jPanelNeuronBlockDesc, BorderLayout.CENTER);
        jPanelNeuronExtraHocBlock.add(jScrollPaneNeuronBlock, BorderLayout.SOUTH);

        jPanelGenesisExtraBlock.add(jPanelCBGenesisExtraBlocks, BorderLayout.NORTH);
        jPanelGenesisExtraBlock.add(jPanelGenesisBlockDesc, BorderLayout.CENTER);
        jPanelGenesisExtraBlock.add(jScrollPaneGenesisBlock, BorderLayout.SOUTH);



        jScrollPaneNeuronBlock.getViewport().add(jTextAreaNeuronBlock, null);
        jScrollPaneGenesisBlock.getViewport().add(jTextAreaGenesisBlock, null);


        ///jPanelNeuronExtraHoc.add(jPanelNeuronExtraHocAfter, null);
        ///jPanelNeuronExtraHocAfter.add(jLabelNeuronExtraAfter, null);
        ///jPanelNeuronExtraHocAfter.add(jScrollPaneNeuronAfter, null);
        ////jScrollPaneNeuronAfter.getViewport().add(jTextAreaNeuronAfter, null);



        jPanelSimulationParams.add(jPanelSimulationGlobal,  BorderLayout.CENTER);

        jPanelSimulationGlobal.add(new JLabel("Default values of biophysical properties of new cells and simulation temperature"),      new GridBagConstraints(0, 0, 3, 1, 0.0, 0.0
            ,GridBagConstraints.CENTER, GridBagConstraints.NONE, new Insets(6, 14, 6, 14), 0, 0));
        jPanelSimulationGlobal.add(new JLabel("Note: each cell can have its own initial potential, axial resistance, etc."),      new GridBagConstraints(0, 1, 3, 1, 0.0, 0.0
            ,GridBagConstraints.CENTER, GridBagConstraints.NONE, new Insets(6, 14, 6, 14), 0, 0));

        jPanelSimulationGlobal.add(jLabelSimulationInitVm,      new GridBagConstraints(0, 2, 1, 1, 0.0, 0.0
            ,GridBagConstraints.WEST, GridBagConstraints.NONE, new Insets(6, 14, 6, 14), 0, 0));
        jPanelSimulationGlobal.add(jTextFieldSimulationInitVm,          new GridBagConstraints(1, 2, 1, 1, 0.0, 0.0
            ,GridBagConstraints.WEST, GridBagConstraints.NONE, new Insets(6, 14, 6, 14), 0, 0));
        jPanelSimulationGlobal.add(jTextFieldSimUnitInitVm,  new GridBagConstraints(2, 2, 1, 1, 0.0, 0.0
            ,GridBagConstraints.WEST, GridBagConstraints.NONE, new Insets(0, 6, 0, 14), 0, 0));



        jPanelSimulationGlobal.add(jLabelSimulationVLeak,      new GridBagConstraints(0, 3, 1, 1, 0.0, 0.0
            ,GridBagConstraints.WEST, GridBagConstraints.NONE, new Insets(6, 14, 6, 14), 0, 0));
        jPanelSimulationGlobal.add(jTextFieldSimulationVLeak,        new GridBagConstraints(1, 3, 1, 1, 0.0, 0.0
            ,GridBagConstraints.WEST, GridBagConstraints.NONE, new Insets(6, 14, 6, 14), 0, 0));
        jPanelSimulationGlobal.add(jTextFieldSimUnitVLeak,     new GridBagConstraints(2, 3, 1, 1, 0.0, 0.0
            ,GridBagConstraints.WEST, GridBagConstraints.NONE, new Insets(0, 6, 0, 14), 0, 0));



        jPanelSimulationGlobal.add(jLabelSimulationGlobRa,     new GridBagConstraints(0, 4, 1, 1, 0.0, 0.0
            ,GridBagConstraints.WEST, GridBagConstraints.NONE, new Insets(6, 14, 6, 14), 0, 0));
        jPanelSimulationGlobal.add(jTextFieldSimulationGlobRa,         new GridBagConstraints(1, 4, 1, 1, 0.0, 0.0
            ,GridBagConstraints.WEST, GridBagConstraints.NONE, new Insets(6, 14, 6, 14), 0, 0));
        jPanelSimulationGlobal.add(jTextFieldSimUnitGlobRa, new GridBagConstraints(2, 4, 1, 1, 0.0, 0.0
            ,GridBagConstraints.WEST, GridBagConstraints.NONE, new Insets(0, 6, 0, 14), 0, 0));


        jPanelSimulationGlobal.add(jLabelSimulationGlobCm,     new GridBagConstraints(0, 5, 1, 1, 0.0, 0.0
            ,GridBagConstraints.WEST, GridBagConstraints.NONE, new Insets(6, 14, 6, 14), 0, 0));
        jPanelSimulationGlobal.add(jTextFieldSimulationGlobCm,        new GridBagConstraints(1, 5, 1, 1, 0.0, 0.0
            ,GridBagConstraints.WEST, GridBagConstraints.NONE, new Insets(6, 14, 6, 14), 0, 0));
        jPanelSimulationGlobal.add(jTextFieldSimUnitGlotCm, new GridBagConstraints(2, 5, 1, 1, 0.0, 0.0
            ,GridBagConstraints.WEST, GridBagConstraints.NONE, new Insets(0, 6, 0, 14), 0, 0));



        jPanelSimulationGlobal.add(jLabelSimulationGlobRm,     new GridBagConstraints(0, 6, 1, 1, 0.0, 0.0
            ,GridBagConstraints.WEST, GridBagConstraints.NONE, new Insets(6, 14, 6, 14), 0, 0));
        jPanelSimulationGlobal.add(jTextFieldSimulationGlobRm,        new GridBagConstraints(1, 6, 1, 1, 0.0, 0.0
            ,GridBagConstraints.WEST, GridBagConstraints.NONE, new Insets(6, 14, 6, 14), 0, 0));
        jPanelSimulationGlobal.add(jTextFieldSimUnitGlobRm,    new GridBagConstraints(2, 6, 1, 1, 0.0, 0.0
            ,GridBagConstraints.WEST, GridBagConstraints.NONE, new Insets(0, 6, 0, 14), 0, 0));



        jPanelSimulationGlobal.add(jLabelSimulationTemp,      new GridBagConstraints(0, 7, 1, 1, 0.0, 0.0
            ,GridBagConstraints.WEST, GridBagConstraints.NONE, new Insets(6, 14, 6, 14), 0, 0));
        jPanelSimulationGlobal.add(jTextFieldSimulationTemperature,        new GridBagConstraints(1, 7, 1, 1, 0.0, 0.0
            ,GridBagConstraints.WEST, GridBagConstraints.NONE, new Insets(6, 14, 6, 14), 0, 0));
        jPanelSimulationGlobal.add(jTextFieldSimUnitTemp,   new GridBagConstraints(2, 7, 1, 1, 0.0, 0.0
            ,GridBagConstraints.WEST, GridBagConstraints.NONE, new Insets(0, 6, 0, 14), 0, 0));
















       ////////// jPanelSimStorage.add(jCheckBoxNeuronSaveHoc, null);

        //jTabbedPaneSimulators.add(jPanelMainInfo,  "MorphML");

        jComboBoxAnalyseCellGroup.addItem(defaultAnalyseCellGroupString);
        jComboBoxAnalyseNetConn.addItem(defaultAnalyseNetConnString);

        //jComboBoxView3DChoice.addItem(choice3DChoiceMain);
        jComboBoxView3DChoice.addItem(LATEST_GENERATED_POSITIONS);
        //jComboBoxPositionsChoice.set

        jComboBoxView3DChoice.setEnabled(false);
        jTabbedPaneGenesis.setSelectedComponent(jPanelGenesisMain);

        /*
        jPanelGenesisExtraBefore.add(jLabelGenesisExtraBefore, null);
        jPanelGenesisExtraBefore.add(jScrollPaneGenesisBefore, null);

        jScrollPaneGenesisBefore.getViewport().add(jTextAreaGenesisBefore, null);

        jPanelGenesisExtra.add(jPanelGenesisExtraBefore, null);
        jPanelGenesisExtra.add(jPanelGenesisExtraAfter, null);
        jPanelGenesisExtraAfter.add(jLabelGenesisExtraAfter, null);
        jPanelGenesisExtraAfter.add(jScrollPaneGenesisAfter, null);
        jScrollPaneGenesisAfter.getViewport().add(jTextAreaGenesisAfter, null);
*/

        jTabbedPaneGenesis.add(jPanelGenesisExtra,  "Extra code");
        jPanelNeuronGraphOptions.add(jCheckBoxNeuronShowShapePlot, null);
        jPanelNeuronGraphOptions.add(jCheckBoxNeuronComments, null);
        jPanelNeuronGraphOptions.add(this.jCheckBoxNeuronNoGraphicsMode, null);


        jPanelSimWhatToRecord.add(jPanelSimRecordWhere,  BorderLayout.SOUTH);
        jPanelSimRecordWhere.add(jLabelSimWhatToRecord, null);
        jPanelSimRecordWhere.add(jRadioButtonSimSomaOnly, null);
        jPanelSimRecordWhere.add(jRadioButtonSimAllSegments, null);
        jPanelSimRecordWhere.add(jRadioButtonSimAllSections, null);
        ////jPanelSimWhatToRecord.add(jPanelSimStorage, BorderLayout.CENTER);
        buttonGroupSimWhatToRecord.add(jRadioButtonSimSomaOnly);
        buttonGroupSimWhatToRecord.add(jRadioButtonSimAllSegments);
        buttonGroupSimWhatToRecord.add(jRadioButtonSimAllSections);
        jPanelGenesisUnits.add(jRadioButtonGenesisSI, null);
        jPanelGenesisUnits.add(jRadioButtonGenesisPhy, null);
        jPanelGenesisSettings.add(jPanelGenesisChoices, BorderLayout.SOUTH);
        buttonGroupGenesisUnits.add(jRadioButtonGenesisSI);
        buttonGroupGenesisUnits.add(jRadioButtonGenesisPhy);

        jPanelCellMechanisms.add(jPanelMechanismLabel, BorderLayout.NORTH);
        jPanelCellMechanisms.add(jPanelMechanismMain,  BorderLayout.CENTER);

        jPanelMechanismMain.add(jScrollPaneMechanisms,  BorderLayout.CENTER);


        jScrollPaneMechanisms.getViewport().add(jTableMechanisms, null);


        jPanelProcessButtons.setLayout(new BorderLayout());

        jPanelProcessButtons.add(jPanelProcessButtonsTop, BorderLayout.CENTER);
        jPanelProcessButtons.add(jPanelProcessButtonsBottom, BorderLayout.SOUTH);

        //////////jPanelProcessButtonsTop.add(jButtonMechanismAbstract, null);
        jPanelProcessButtonsTop.add(jButtonMechanismFileBased, null);
        jPanelProcessButtonsTop.add(this.jButtonMechanismNewCML, null);
        jPanelProcessButtonsTop.add(this.jButtonMechanismTemplateCML, null);


        jPanelProcessButtonsBottom.add(jButtonMechanismEdit, null);
        jPanelProcessButtonsBottom.add(jButtonMechanismDelete, null);

        jPanelMechanismLabel.add(jPanelProcessButtons,  BorderLayout.SOUTH);
        jPanelMechanismLabel.add(JLabelMechanismMain,  BorderLayout.NORTH);

        jPanelGenesisChoices.add(jPanelGenesisUnits, BorderLayout.NORTH);
        jPanelGenesisChoices.add(jPanelGenesisNumMethod,  BorderLayout.SOUTH);

        jPanelGenesisSettings.add(jPanelGenesisCheckBoxes, BorderLayout.CENTER);

        jPanelGenesisCheckBoxes.add(jCheckBoxGenesisShapePlot, null);
        jPanelGenesisCheckBoxes.add(jCheckBoxGenesisSymmetric, null);
        jPanelGenesisCheckBoxes.add(jCheckBoxGenesisComments, null);
        jPanelGenesisCheckBoxes.add(jCheckBoxGenesisNoGraphicsMode, null);

        jPanelGenesisNumMethod.add(jLabelGenesisNumMethod, null);
        jPanelGenesisNumMethod.add(jButtonGenesisNumMethod, null);
        jPanelSimValsInput.add(jPanelSimDT,  BorderLayout.CENTER);
        jPanelSimPlotButtons.add(jButtonSimPlotDelete, null);
        jPanelStims.add(jScrollPaneSimStims, BorderLayout.CENTER);
        jPanelSimStimButtons.add(jButtonSimStimAdd, null);
        jPanelSimStimButtons.add(jButtonSimStimEdit, null);
        jPanelSimStimButtons.add(jButtonSimStimDelete, null);



        jMenuProject.add(jMenuItemGenNetwork);
        jMenuProject.add(jMenuItemGenNeuron);
        jMenuProject.add(jMenuItemGenGenesis);
        jMenuProject.addSeparator();
        jMenuProject.add(jMenuItemPrevSims);
        jMenuProject.add(jMenuItemDataSets);

        jTabbedPaneMain.addTab(PROJECT_INFO_TAB, null, jPanelProjInfo, toolTipText.getToolTip("Project Info Tab"));
        jTabbedPaneMain.addTab(CELL_TYPES_TAB, null, jPanelCellTypes, toolTipText.getToolTip("Cell Type Tab"));
        jTabbedPaneMain.addTab(REGIONS_TAB, null, jPanelRegions, toolTipText.getToolTip("Region"));
        jTabbedPaneMain.addTab(CELL_GROUPS_TAB, null, jPanelCellGroupDetails, toolTipText.getToolTip("Cell Groups Tab"));
        jTabbedPaneMain.addTab(CELL_MECHANISM_TAB, null, jPanelCellMechanisms, toolTipText.getToolTip("Cell Mechanism"));

        jTabbedPaneMain.addTab(NETWORK_TAB, null, jPanelNetworkSettings, toolTipText.getToolTip("Network Tab"));

        jTabbedPaneMain.addTab(INPUT_OUTPUT_TAB, null, jPanelInputOutput, toolTipText.getToolTip("Input Output Tab"));

        jTabbedPaneMain.addTab(GENERATE_TAB, null, jPanelGenerate, toolTipText.getToolTip("Generate Tab"));
        jTabbedPaneMain.addTab(VISUALISATION_TAB, null, jPanel3DDemo, toolTipText.getToolTip("Visualisation Tab"));
        jTabbedPaneMain.addTab(EXPORT_TAB, null, jPanelExport, toolTipText.getToolTip("Export Tab"));


        jTabbedPaneExportFormats.add(jPanelSimGeneral, SIMULATOR_SETTINGS_TAB);
        jTabbedPaneExportFormats.add(jPanelExportNeuron, NEURON_SIMULATOR_TAB);
        jTabbedPaneNeuron.add(jPanelNeuronMainSettings, NEURON_TAB_GENERATE);
        jTabbedPaneNeuron.add(jPanelNeuronExtraHoc, NEURON_TAB_EXTRA);
        jTabbedPaneExportFormats.add(jPanelExportGenesis, GENESIS_SIMULATOR_TAB);


        jTabbedPaneExportFormats.add(jPanelNeuroML, MORPHML_TAB);



        jTextFieldSimUnitInitVm.setText(UnitConverter.voltageUnits[UnitConverter.NEUROCONSTRUCT_UNITS].getSymbol());
        jTextFieldSimUnitVLeak.setText(UnitConverter.voltageUnits[UnitConverter.NEUROCONSTRUCT_UNITS].getSymbol());
        jTextFieldSimUnitGlobRa.setText(UnitConverter.specificAxialResistanceUnits[UnitConverter.NEUROCONSTRUCT_UNITS].getSymbol());
        jTextFieldSimUnitGlotCm.setText(UnitConverter.specificCapacitanceUnits[UnitConverter.NEUROCONSTRUCT_UNITS].getSymbol());
        jTextFieldSimUnitGlobRm.setText(UnitConverter.specificMembraneResistanceUnits[UnitConverter.NEUROCONSTRUCT_UNITS].getSymbol());
        jTextFieldSimUnitTemp.setText(Units.CELSIUS.getSymbol());


    }


    /**
     * Extra initiation stuff, not automatically added by the IDE
     */
    private void extraInit()
    {
        // Make sure menus come to front (especially in front of 3D panel...)
        JPopupMenu.setDefaultLightWeightPopupEnabled(false);

        jComboBoxView3DChoice.setLightWeightPopupEnabled(false);
        //jComboBox.setLightWeightPopupEnabled(false);

        ToolTipManager ttm = ToolTipManager.sharedInstance();
        ttm.setDismissDelay(12000);
        ttm.setReshowDelay(100);

        /** @todo Put these in gen props... */
        jTextFieldIClampAmplitude.setText("0.4");
        jTextFieldIClampDuration.setText("100");

        /** @todo Put these in gen props... */
        jTextFieldNetStimNumber.setText("10");
        jTextFieldNetStimNoise.setText("0.7");

        jTabbedPaneMain.setBackground(Color.lightGray);

        JViewport vp = jScrollPaneProjDesc.getViewport();

        vp.add(jTextAreaProjDescription);
        jTextAreaProjDescription.setWrapStyleWord(true);

        ArrayList<MorphCompartmentalisation> mcs = CompartmentalisationManager.getAllMorphProjections();

        for (int i = 0; i < mcs.size(); i++)
        {
            this.jComboBoxNeuroMLComps.addItem(mcs.get(i));
            this.jComboBoxGenesisComps.addItem(mcs.get(i));
            if (mcs.get(i).getName().toUpperCase().indexOf("GENESIS")>=0)
            {
                jComboBoxGenesisComps.setSelectedIndex(i);
            }
        }
        //jComboBoxGenesisComps.setSelectedItem();


        addNamedDocumentListner(PROJECT_INFO_TAB, jTextAreaProjDescription);

        // addNamedDocumentListner(REGIONS_TAB, jTextFieldDepth);
        //   addNamedDocumentListner(REGIONS_TAB, jTextFieldWidth);



        addCheckBoxListner(NEURON_SIMULATOR_TAB, jCheckBoxNeuronShowShapePlot);
        addCheckBoxListner(NEURON_SIMULATOR_TAB, this.jCheckBoxNeuronNoGraphicsMode);

        addCheckBoxListner(NEURON_SIMULATOR_TAB, jCheckBoxNeuronComments);

        addNamedDocumentListner(NEURON_SIMULATOR_TAB, jTextFieldSimRef);
        addNamedDocumentListner(NEURON_SIMULATOR_TAB, jTextFieldSimDefDur);
        addNamedDocumentListner(NEURON_SIMULATOR_TAB, jTextFieldSimDT);


        addNamedDocumentListner(NEURON_SIMULATOR_TAB, jTextFieldIClampAmplitude);
        addNamedDocumentListner(NEURON_SIMULATOR_TAB, jTextFieldIClampDuration);
        addNamedDocumentListner(NEURON_SIMULATOR_TAB, jTextFieldNetStimNoise);
        addNamedDocumentListner(NEURON_SIMULATOR_TAB, jTextFieldNetStimNumber);

        addRadioButtonListner(NEURON_SIMULATOR_TAB, jRadioButtonNeuronSimSaveToFile);
        addRadioButtonListner(NEURON_SIMULATOR_TAB, jRadioButtonNeuronSimDontRecord);


        addRadioButtonListner(NEURON_SIMULATOR_TAB, jRadioButtonSimAllSegments);
        addRadioButtonListner(NEURON_SIMULATOR_TAB, jRadioButtonSimSomaOnly);
        addRadioButtonListner(NEURON_SIMULATOR_TAB, jRadioButtonSimAllSections);




        addNamedDocumentListner(NEURON_SIMULATOR_TAB, jTextFieldSimulationGlobCm);
        addNamedDocumentListner(NEURON_SIMULATOR_TAB, jTextFieldSimulationGlobRa);
        addNamedDocumentListner(NEURON_SIMULATOR_TAB, jTextFieldSimulationGlobRm);
        addNamedDocumentListner(NEURON_SIMULATOR_TAB, jTextFieldSimulationVLeak);
        addNamedDocumentListner(NEURON_SIMULATOR_TAB, jTextFieldSimulationInitVm);
        addNamedDocumentListner(NEURON_SIMULATOR_TAB, jTextFieldSimulationTemperature);


        addCheckBoxListner(NEURON_SIMULATOR_TAB, jCheckBoxSpecifySimRef);
        addCheckBoxListner(NEURON_SIMULATOR_TAB, jCheckBoxNeuronSaveHoc);
        addCheckBoxListner(NEURON_SIMULATOR_TAB, jCheckBoxNeuronNumInt);

        addNamedDocumentListner(NEURON_SIMULATOR_TAB, jTextAreaNeuronBlock);
        ////addNamedDocumentListner(NEURON_SIMULATOR_TAB, jTextAreaNeuronAfter);




        //////addNamedDocumentListner(GENESIS_SIMULATOR_TAB, jTextAreaGenesisBefore);
        ////////addNamedDocumentListner(GENESIS_SIMULATOR_TAB, jTextAreaGenesisAfter);


        addCheckBoxListner(GENESIS_SIMULATOR_TAB, jCheckBoxGenesisSymmetric);
        addCheckBoxListner(GENESIS_SIMULATOR_TAB, jCheckBoxGenesisComments);
        //addCheckBoxListner(GENESIS_SIMULATOR_TAB, jCheckBoxGenesisVoltPlot);
        addCheckBoxListner(GENESIS_SIMULATOR_TAB, jCheckBoxGenesisShapePlot);
        addCheckBoxListner(GENESIS_SIMULATOR_TAB, jCheckBoxGenesisNoGraphicsMode);

        addRadioButtonListner(GENESIS_SIMULATOR_TAB, jRadioButtonGenesisPhy);
        addRadioButtonListner(GENESIS_SIMULATOR_TAB, jRadioButtonGenesisSI);
        addNamedDocumentListner(GENESIS_SIMULATOR_TAB, jTextAreaGenesisBlock);





    }


    /**
     * A simple check on wether to incl parallel functionality...
     */
    private boolean includeParallelFunc()
    {
        return new File("parallel").exists(); // A file of that name in the neuroConstruct main dir...
    }


    private void addToolTips()
    {
        jButtonSaveProject.setToolTipText("Save Project");
        jButtonOpenProject.setToolTipText("Open Project");
        jButtonCloseProject.setToolTipText("Close Project");
        jButtonNewProject.setToolTipText("Project");
        jButtonPreferences.setToolTipText("Preferences");

        jButtonValidate.setToolTipText(toolTipText.getToolTip("Project validity"));


        jButtonCellTypeNew.setToolTipText(toolTipText.getToolTip("Add New Cell Type"));

        jButtonCellTypeOtherProject.setToolTipText(toolTipText.getToolTip("Add New Cell Type From Other Project"));

        jButtonCellTypeBioPhys.setToolTipText(toolTipText.getToolTip("Edit Cell Biophysics"));

        jButtonRegionNew.setToolTipText(toolTipText.getToolTip("Add New Region"));

        jButtonCellGroupsNew.setToolTipText(toolTipText.getToolTip("Cell Group"));

        jButtonMechanismAbstract.setToolTipText(toolTipText.getToolTip("Abstracted Cell Mechanisms"));
        jButtonMechanismFileBased.setToolTipText(toolTipText.getToolTip("File based Cell Mechanisms"));

        jMenuItemNewProject.setToolTipText(toolTipText.getToolTip("Project"));
        jMenuItemFileOpen.setToolTipText(toolTipText.getToolTip("Open project"));
        jMenuItemSaveProject.setToolTipText(toolTipText.getToolTip("Save project"));
        jMenuItemCloseProject.setToolTipText(toolTipText.getToolTip("Close project"));
        jMenuItemCopyProject.setToolTipText(toolTipText.getToolTip("Copy project"));
        jMenuItemZipUp.setToolTipText(toolTipText.getToolTip("Zipped Project"));
        jMenuItemUnzipProject.setToolTipText(toolTipText.getToolTip("Import project"));

        jMenuItemDataSets.setToolTipText(toolTipText.getToolTip("Data Set Manager"));

        jMenuItemProjProperties.setToolTipText(toolTipText.getToolTip("Project properties"));
        jMenuItemGeneralProps.setToolTipText(toolTipText.getToolTip("General properties"));


        jMenuItemNmodlEditor.setToolTipText(toolTipText.getToolTip("nmodlEditor menu item"));
        jMenuItemViewProjSource.setToolTipText(toolTipText.getToolTip("Project file source"));
        jMenuItemCondorMonitor.setToolTipText(toolTipText.getToolTip("Condor monitor"));
        jMenuItemGlossary.setToolTipText(toolTipText.getToolTip("Glossary menu item"));
        jMenuItemUnits.setToolTipText(toolTipText.getToolTip("Units menu item"));


        ///jPanelChanMechsButtons.setToolTipText(toolTipText.getToolTip("Channel Mechanism"));
        jPanelSynapseButtons.setToolTipText(toolTipText.getToolTip("Synaptic Mechanism"));
        jPanelNetSetControls.setToolTipText(toolTipText.getToolTip("Network Connection"));

        //jPanelNetSetComplexControls.setToolTipText(toolTipText.getToolTip("Complex Network Connection"));

        jButtonNetSetAddNew.setToolTipText(toolTipText.getToolTip("Network Connection"));

        //jButtonNetAAAdd.setToolTipText(toolTipText.getToolTip("Complex Network Connection"));

        jButton3DView.setToolTipText(toolTipText.getToolTip("View 3D"));
        this.jComboBoxView3DChoice.setToolTipText(toolTipText.getToolTip("View 3D"));

        this.jButton3DSettings.setToolTipText("Settings for displaying cells and networks in 3D. "
                                              +"For more go to Help -> Glossary -> 3D View of Cells.");

        this.jButton3DDestroy.setToolTipText("Close the 3D view.");



        jButton3DPrevSims.setToolTipText(toolTipText.getToolTip("Previous Simulations"));
        jMenuItemPrevSims.setToolTipText(toolTipText.getToolTip("Previous Simulations"));

        jButtonSimStimAdd.setToolTipText(toolTipText.getToolTip("Elec Input"));

        String newSavingTip = "Note: to save values calculated during a simulation, go to Input and Output";

        jLabelSimWhatToRecord.setToolTipText(newSavingTip);
        jRadioButtonSimAllSegments.setToolTipText(newSavingTip);
        jRadioButtonSimSomaOnly.setToolTipText(newSavingTip);

        this.jRadioButtonNeuronSimDontRecord.setToolTipText(newSavingTip);

        this.jRadioButtonNeuronSimSaveToFile.setToolTipText(newSavingTip);

        jCheckBoxGenesisShapePlot.setToolTipText(toolTipText.getToolTip("GENESIS 3D"));
        this.jCheckBoxGenesisSymmetric.setToolTipText(toolTipText.getToolTip("GENESIS Symmetric"));
        this.jRadioButtonGenesisPhy.setToolTipText(toolTipText.getToolTip("GENESIS Units"));
        this.jRadioButtonGenesisSI.setToolTipText(toolTipText.getToolTip("GENESIS Units"));
        this.jLabelGenesisNumMethod.setToolTipText(toolTipText.getToolTip("GENESIS Num Integration method"));

        this.jButtonSimConfigEdit.setToolTipText(toolTipText.getToolTip("Simulation Configuration"));

        this.jLabelSimConfigs.setToolTipText(toolTipText.getToolTip("Click sim config"));
        this.jPanelSimConfigClicks.setToolTipText(toolTipText.getToolTip("Click sim config"));



        jButtonCellTypeCopy.setToolTipText(toolTipText.getToolTip("Cell Type Copy"));
        jButtonCellTypeDelete.setToolTipText(toolTipText.getToolTip("Cell Type Delete"));
        jButtonCellTypeCompare.setToolTipText(toolTipText.getToolTip("Cell Type Compare"));
        jButtonCellTypesMoveToOrigin.setToolTipText(toolTipText.getToolTip("Cell Type Move"));
        jButtonCellTypeViewCellInfo.setToolTipText(toolTipText.getToolTip("Cell Type View Cell Info"));
        jButtonCellTypeViewCell.setToolTipText(toolTipText.getToolTip("Cell Type View Cell in 3D"));

        jButtonCellTypeViewCellChans.setToolTipText(toolTipText.getToolTip("Cell Type View Memb Mechs"));

        //this.jbutt
        jButtonCellTypesConnect.setToolTipText(toolTipText.getToolTip("Cell Type Connect"));
        jButtonCellTypesMakeSimpConn.setToolTipText(toolTipText.getToolTip("Cell Type Make Simply Connected"));

        jTextFieldSimRef.setToolTipText(toolTipText.getToolTip("Simulation Reference"));
        jLabelSimRef.setToolTipText(toolTipText.getToolTip("Simulation Reference"));
        jCheckBoxSpecifySimRef.setToolTipText(toolTipText.getToolTip("Specify Reference"));


        this.jCheckBoxNeuronNoGraphicsMode.setToolTipText(toolTipText.getToolTip("No Graphics Mode"));
        this.jCheckBoxGenesisNoGraphicsMode.setToolTipText(toolTipText.getToolTip("No Graphics Mode"));

        this.jButtonMechanismNewCML.setToolTipText(toolTipText.getToolTip("File Based ChannelML"));
        this.jButtonMechanismTemplateCML.setToolTipText(toolTipText.getToolTip("Template Based ChannelML"));


        jLabelSimulationInitVm.setToolTipText(toolTipText.getToolTip("Initial Membrane Potential"));
        jTextFieldSimulationInitVm.setToolTipText(toolTipText.getToolTip("Initial Membrane Potential"));
        jLabelSimulationVLeak.setToolTipText(toolTipText.getToolTip("Global Membrane Leakage Potential"));
        jTextFieldSimulationVLeak.setToolTipText(toolTipText.getToolTip("Global Membrane Leakage Potential"));
        jLabelSimulationGlobRa.setToolTipText(toolTipText.getToolTip("Global specific axial resistance"));
        jTextFieldSimulationGlobRa.setToolTipText(toolTipText.getToolTip("Global specific axial resistance"));
        jLabelSimulationGlobCm.setToolTipText(toolTipText.getToolTip("Global specific membrane capacitance"));
        jTextFieldSimulationGlobCm.setToolTipText(toolTipText.getToolTip("Global specific membrane capacitance"));
        jLabelSimulationGlobRm.setToolTipText(toolTipText.getToolTip("Global specific membrane resistance"));
        jTextFieldSimulationGlobRm.setToolTipText(toolTipText.getToolTip("Global specific membrane resistance"));
        jTextFieldSimulationTemperature.setToolTipText(toolTipText.getToolTip("Simulation Temperature"));

        this.jCheckBoxGenesisComments.setToolTipText(toolTipText.getToolTip("Generate comments"));
        this.jCheckBoxNeuronComments.setToolTipText(toolTipText.getToolTip("Generate comments"));
        this.jCheckBoxNeuronShowShapePlot.setToolTipText(toolTipText.getToolTip("NEURON 3D"));

        jCheckBoxNeuronNumInt.setToolTipText(toolTipText.getToolTip("NeuronNumInt"));


        jLabelSimDefDur.setToolTipText(toolTipText.getToolTip("Simulation def duration"));
        this.jTextFieldSimDefDur.setToolTipText(toolTipText.getToolTip("Simulation def duration"));

        this.jLabelSimDT.setToolTipText(toolTipText.getToolTip("Simulation dt"));
        this.jTextFieldSimDT.setToolTipText(toolTipText.getToolTip("Simulation dt"));


        this.jButtonGenerateSave.setToolTipText(toolTipText.getToolTip("Save NetworkML"));
        this.jButtonGenerateLoad.setToolTipText(toolTipText.getToolTip("Load NetworkML"));
        this.jCheckBoxGenerateZip.setToolTipText(toolTipText.getToolTip("Compress NetworkML"));
        this.jCheckBoxGenerateExtraNetComments.setToolTipText(toolTipText.getToolTip("Extra comments NetworkML"));

        this.jLabelGenesisCompsDesc.setToolTipText(toolTipText.getToolTip("Compartmentalisation"));
        this.jComboBoxGenesisComps.setToolTipText(toolTipText.getToolTip("Compartmentalisation"));

    }

    @SuppressWarnings("serial")
    private void enableTableCellEditingFunctionality()
    {
        DefaultTableCellRenderer colorRenderer = new DefaultTableCellRenderer()
        {
            public void setValue(Object value)
            {
                if (value instanceof Color)
                {
                    setBackground( (Color) value);
                }
                else
                {
                    super.setValue(value);
                }
            }
        };

        TableColumn colorColumn
            = jTableCellGroups.getColumn(jTableCellGroups.getColumnName(CellGroupsInfo.
            COL_NUM_COLOUR));

        colorColumn.setCellRenderer(colorRenderer);

        colorColumn.setCellEditor(new CellGroupColourEditor());

        DefaultTableCellRenderer regionColourRenderer = new DefaultTableCellRenderer()
        {
            public void setValue(Object value)
            {
                if (value instanceof Color)
                {
                    setBackground( (Color) value);
                }
                else
                {
                    super.setValue(value);
                }
            }
        };

        TableColumn regionColorColumn
            = jTable3DRegions.getColumn(jTable3DRegions.getColumnName(RegionsInfo.
                                                                        COL_NUM_COLOUR));

        regionColorColumn.setCellRenderer(regionColourRenderer);
        regionColorColumn.setMaxWidth(80);


        regionColorColumn.setCellEditor(new RegionColourEditor());



        TableColumn adapterColumn
            = jTableCellGroups.getColumn(jTableCellGroups.getColumnName(CellGroupsInfo.
            COL_NUM_PACKING_ADAPTER));

      //  adapterColumn.setCellRenderer(adapterRenderer);

        adapterColumn.setCellEditor(new AdapterEditor(this));


        jTableCellGroups.getColumn(jTableCellGroups.getColumnName(CellGroupsInfo.COL_NUM_COLOUR)).setPreferredWidth(60);



        jTableCellGroups.getColumn(jTableCellGroups.getColumnName(CellGroupsInfo.COL_NUM_PACKING_ADAPTER)).setMinWidth(220);

        DropDownObjectCellEditor enabledEditor = new DropDownObjectCellEditor();
        enabledEditor.addValue(Boolean.TRUE);
        enabledEditor.addValue(Boolean.FALSE);



        ///jTableCellGroups.getColumn(jTableCellGroups.getColumnName(CellGroupsInfo.COL_NUM_ENABLED)).setCellEditor(enabledEditor);

        this.jTableMechanisms.getColumn(jTableMechanisms.getColumnName(CellMechanismInfo.COL_NUM_DESC)).setMinWidth(220);

    }

    public void tableDataModelUpdated(String tableModelName)
    {
        logger.logComment("Being told table :(" + tableModelName + ") is being updated...");
        if (initialisingProject)
        {
            logger.logComment("Ignoring because the project is being loaded...");
        }
        else
        {
            logger.logComment("Refreshing...");
            this.refreshGeneral();
            //if (base3DPanel!=null) refreshTab3D();
        }

    }

    public void cellMechanismUpdated()
    {
        try
        {
            projManager.getCurrentProject().cellMechanismInfo.reinitialiseCMLMechs(projManager.getCurrentProject());
        }
        catch (ChannelMLException ex1)
        {
            GuiUtils.showErrorMessage(logger,
                                      "Error reinitialising Cell Mechanisms: " +
                                      ex1.getMessage(),
                                      ex1,
                                      null);

        }

        projManager.getCurrentProject().markProjectAsEdited();
        this.refreshTabCellProcesses();
    }

    public void tabUpdated(String tableModelName)
    {
        logger.logComment("Tab updated: " + tableModelName);

        if (projManager.projectLoaded() && !initialisingProject &&
            projManager.getCurrentProject().getProjectStatus() != Project.PROJECT_NOT_INITIALISED)
        {
            logger.logComment("Updating stuff in that table...");

            if (tableModelName.equals(PROJECT_INFO_TAB))
            {
                projManager.getCurrentProject().setProjectDescription(jTextAreaProjDescription.getText());
            }
            else if (tableModelName.equals(REGIONS_TAB))
            {
                /*
                if (jTextFieldDepth.getText().length() > 0)
                {
                    projManager.getCurrentProject().regionsInfo.setRegionDepth(Float.parseFloat(jTextFieldDepth.
                        getText()));
                }
                if (jTextFieldWidth.getText().length() > 0)
                {
                    projManager.getCurrentProject().regionsInfo.setRegionWidth(Float.parseFloat(jTextFieldWidth.
                        getText()));
                }
*/
                this.refreshTabRegionsInfo();
            }
            else if (tableModelName.equals(GENESIS_SIMULATOR_TAB))
            {
                //projManager.getCurrentProject().genesisSettings.setTextAfterCellCreation(jTextAreaGenesisAfter.getText());
                //projManager.getCurrentProject().genesisSettings.setTextBeforeCellCreation(jTextAreaGenesisBefore.getText());

                if (!jComboBoxGenesisExtraBlocks.getSelectedItem().equals(this.genesisBlockPrompt))
                {
                    ScriptLocation currNcl = (ScriptLocation)this.jComboBoxGenesisExtraBlocks.getSelectedItem();
                    projManager.getCurrentProject().genesisSettings.setNativeBlock(currNcl, jTextAreaGenesisBlock.getText());
                }

                projManager.getCurrentProject().genesisSettings.setGraphicsMode(!this.jCheckBoxGenesisNoGraphicsMode.isSelected());


                projManager.getCurrentProject().genesisSettings.setSymmetricCompartments(jCheckBoxGenesisSymmetric.isSelected());
                projManager.getCurrentProject().genesisSettings.setGenerateComments(jCheckBoxGenesisComments.isSelected());
                projManager.getCurrentProject().genesisSettings.setShowShapePlot(jCheckBoxGenesisShapePlot.isSelected());
                ////////projManager.getCurrentProject().genesisSettings.setShowVoltPlot(jCheckBoxGenesisVoltPlot.isSelected());

                if (jRadioButtonGenesisPhy.isSelected())
                    projManager.getCurrentProject().genesisSettings.setUnitSystemToUse(UnitConverter.GENESIS_PHYSIOLOGICAL_UNITS);
                if (jRadioButtonGenesisSI.isSelected())
                    projManager.getCurrentProject().genesisSettings.setUnitSystemToUse(UnitConverter.GENESIS_SI_UNITS);

                projManager.getCurrentProject().markProjectAsEdited();

                this.refreshTabGenesis();

            }
            else if (tableModelName.equals(NEURON_SIMULATOR_TAB))
            {

                projManager.getCurrentProject().neuronSettings.setShowShapePlot(jCheckBoxNeuronShowShapePlot.isSelected());
                projManager.getCurrentProject().neuronSettings.setGraphicsMode(!this.jCheckBoxNeuronNoGraphicsMode.isSelected());
                projManager.getCurrentProject().neuronSettings.setGenerateComments(jCheckBoxNeuronComments.isSelected());

                //System.out.println("ID: "+projManager.getCurrentProject().stimulationSettings.getSegmentID());
                projManager.getCurrentProject().simulationParameters.setReference(jTextFieldSimRef.getText());

                projManager.getCurrentProject().simulationParameters.setSpecifySimName(jCheckBoxSpecifySimRef.isSelected());
                projManager.getCurrentProject().simulationParameters.setSaveCopyGenSimFiles(jCheckBoxNeuronSaveHoc.isSelected());


                ////projManager.getCurrentProject().neuronSettings.setTextAfterCellCreation(jTextAreaNeuronAfter.getText());
                //projManager.getCurrentProject().neuronSettings.setTextBeforeCellCreation(jTextAreaNeuronBlock.getText());

                if (!jComboBoxNeuronExtraBlocks.getSelectedItem().equals(this.neuronBlockPrompt))
                {
                    NativeCodeLocation currNcl = (NativeCodeLocation)this.jComboBoxNeuronExtraBlocks.getSelectedItem();
                    projManager.getCurrentProject().neuronSettings.setNativeBlock(currNcl, jTextAreaNeuronBlock.getText());
                }

                projManager.getCurrentProject().neuronSettings.setVarTimeStep(this.jCheckBoxNeuronNumInt.isSelected());


                try
                {
                    String dur = jTextFieldSimDefDur.getText();
                    String dt = jTextFieldSimDT.getText();

                    String globCm = jTextFieldSimulationGlobCm.getText();
                    String globRa = jTextFieldSimulationGlobRa.getText();
                    String globRm = jTextFieldSimulationGlobRm.getText();
                    String initVm = jTextFieldSimulationInitVm.getText();
                    String globVLeak = jTextFieldSimulationVLeak.getText();
                    String temp = jTextFieldSimulationTemperature.getText();

                    if (globCm.length() > 0 && !globCm.equals("-"))
                        projManager.getCurrentProject().simulationParameters.setGlobalCm(Float.parseFloat(globCm));
                    else
                        projManager.getCurrentProject().simulationParameters.setGlobalCm(0);

                    if (globRa.length() > 0 && !globRa.equals("-"))
                        projManager.getCurrentProject().simulationParameters.setGlobalRa(Float.parseFloat(globRa));
                    else
                        projManager.getCurrentProject().simulationParameters.setGlobalRa(0);

                    if (globRm.length() > 0 && !globRm.equals("-"))
                        projManager.getCurrentProject().simulationParameters.setGlobalRm(Float.parseFloat(globRm));
                    else
                        projManager.getCurrentProject().simulationParameters.setGlobalRm(0);

                    if (initVm.length() > 0 && !initVm.equals("-"))
                        projManager.getCurrentProject().simulationParameters.setInitVm(Float.parseFloat(initVm));
                    else
                        projManager.getCurrentProject().simulationParameters.setInitVm(0);

                    if (globVLeak.length() > 0 && !globVLeak.equals("-"))
                        projManager.getCurrentProject().simulationParameters.setGlobalVLeak(Float.parseFloat(globVLeak));
                    else
                        projManager.getCurrentProject().simulationParameters.setGlobalVLeak(0);


                    if (temp.length() > 0 && !temp.equals("-"))
                        projManager.getCurrentProject().simulationParameters.setTemperature(Float.parseFloat(temp));
                    else
                        projManager.getCurrentProject().simulationParameters.setTemperature(0);


                    if (dur.length()>0)
                        projManager.getCurrentProject().simulationParameters.setDuration(Float.parseFloat(dur));
                    else
                        projManager.getCurrentProject().simulationParameters.setDuration(0);

                    if(dt.length()>0)
                        projManager.getCurrentProject().simulationParameters.setDt(Float.parseFloat(dt));
                    else
                        projManager.getCurrentProject().simulationParameters.setDt(0f);

                }
                catch (NumberFormatException ex)
                {
                    GuiUtils.showErrorMessage(logger,
                                              "Problem reading the Simulation paramters: " +
                                              ex.getMessage(), ex, this);
                    return;
                }
/*
                if (jRadioButtonNeuronSimSaveToFile.isSelected())
                {
                    projManager.getCurrentProject().simulationParameters.setRecordingMode(SimulationParameters.
                        SIMULATION_RECORD_TO_FILE);
                    logger.logComment("recording to file...");
                }
                else if (jRadioButtonNeuronSimDontRecord.isSelected())
                {
                    projManager.getCurrentProject().simulationParameters.setRecordingMode(SimulationParameters.
                        SIMULATION_NOT_RECORDED);
                    logger.logComment("NOT recording to file...");
                }
                else
                {
                    logger.logError("No radio button set!!");
                }*/

                if (jRadioButtonSimSomaOnly.isSelected())
                {
                    projManager.getCurrentProject().simulationParameters.setWhatToRecord(SimulationParameters.RECORD_ONLY_SOMA);
                }/*
                else if (jRadioButtonSimAllSections.isSelected())
                {
                    projManager.getCurrentProject().simulationParameters.whatToRecord =
                        SimulationParameters.RECORD_EVERY_SECTION;

                }*/
                else if (jRadioButtonSimAllSegments.isSelected())
                {
                    projManager.getCurrentProject().simulationParameters.setWhatToRecord(
                                        SimulationParameters.RECORD_EVERY_SEGMENT);
                }


                this.createSimulationSummary();
                projManager.getCurrentProject().markProjectAsEdited();
                refreshTabNeuron();
            }
        }
        else
        {
            logger.logComment("Not updating stuff in that table, as the project is initialising...");
        }

        this.refreshGeneral();
    }


    /*

    if (this.getProjectMainDirectory().getAbsolutePath().equals(ProjectStructure.getExamplesDirectory().getAbsolutePath()))
    {
        int yesNo = GuiUtils.showYesNoMessage(logger, "Note: the project:\n"+this.getProjectFile()
                                  +"\nis one of the included example project in neuroConstruct. These are referenced in the documentation and paper.\n"
                                  +"Are you sure you want to save it? Select No to save under another name.", null);

        if (yesNo!=JOptionPane.YES_OPTION)
        {

        }


    }*/



    /**
     * Carries out all the actions needed when a button or menu item is selected
     * for creating a new project
     *
     */
    private void doNewProject()
    {
        boolean continueClosing = checkToSave();
        if (!continueClosing) return;
        closeProject();

        logger.logComment(">>>>>>>>>>>>>>>    Creating new project...");

        String projName = null;
        String projDir = null;

        NewProjectDialog dlg = new NewProjectDialog(this,
            "New neuroConstruct project", false);
        Dimension dlgSize = dlg.getPreferredSize();
        Dimension frmSize = getSize();
        Point loc = getLocation();
        dlg.setLocation( (frmSize.width - dlgSize.width) / 2 + loc.x,
                        (frmSize.height - dlgSize.height) / 2 + loc.y);
        dlg.setModal(true);
        dlg.pack();
        dlg.setVisible(true);

        projName = dlg.getProjFileName();
        projDir = dlg.getProjDirName();

        if (dlg.cancelled)
        {
            logger.logComment("Cancel pressed...");
            return;
        }
        if (projName.length() == 0 || projDir.length() == 0)
        {
            GuiUtils.showErrorMessage(logger, "No proj name/directory entered", null, this);
            return;
        }

        String projectFileExtension = ProjectStructure.getProjectFileExtension();

        String fileSep = System.getProperty("file.separator");

        String nameNewProjectDir = projDir + fileSep + projName + fileSep;

        File newProjectFile = new File(nameNewProjectDir + projName + projectFileExtension);

        if (newProjectFile.exists())
        {
            logger.logComment("The file " + newProjectFile.getAbsolutePath() + " already exists");
            Object[] options =
                {"OK", "Cancel"};

            JOptionPane option = new JOptionPane(
                "This project file: " + newProjectFile.getAbsolutePath() + " already exists. Overwrite?\nNOTE: This will remove all files in the project directory!",
                JOptionPane.DEFAULT_OPTION,
                JOptionPane.WARNING_MESSAGE,
                null,
                options,
                options[0]);

            JDialog dialog = option.createDialog(this, "Warning");
            dialog.setVisible(true);

            Object choice = option.getValue();
            logger.logComment("User has chosen: " + choice);
            if (choice.equals("Cancel"))
            {
                logger.logComment("User has changed their mind...");
                return;
            }

            cleanseDirectory(newProjectFile.getParentFile());

        }
        else
        {
            logger.logComment("The file " + newProjectFile.getAbsolutePath() + " doesn't already exist");
        }
        this.initialisingProject = true;

        projManager.setCurrentProject(Project.createNewProject(nameNewProjectDir, projName, this));


        recentFiles.addToList(newProjectFile.getAbsolutePath());

        try
        {
            projManager.getCurrentProject().saveProject();
            logger.logComment("Project saved");
        }
        catch (Exception ex)
        {
            logger.logError("Error when saving project", ex);
        }

        int yesNo = JOptionPane.showConfirmDialog(this, "Would you like to add some example Cell Types/Regions/Cell Mechanisms to the project?"
                                                  + "\nThese can all be removed later.", "Add sample items", JOptionPane.YES_NO_OPTION);

        if (yesNo==JOptionPane.YES_OPTION)
        {

            addSampleItems();
        }

        this.refreshAll();
        enableTableCellEditingFunctionality();

        this.initialisingProject = false;
        createSimulationSummary();
        projManager.getCurrentProject().markProjectAsEdited();

        logger.logComment(">>>>>>>>>>>>>>>    Finished creating new project");

        jTabbedPaneMain.setSelectedIndex(0); // main tab...

        this.doSave();

    }


    private void addSampleItems()
    {
        String newCellName = "SampleCell";
        String newRegionName = "SampleRegion";
        String newCellGroupName = "SampleCellGroup";
        String newStim = "SampleIClamp";
        String newPlot = "SamplePlot";

        SimpleCell simpleCell = new SimpleCell(newCellName);

        Cell cell = (Cell)simpleCell.clone();

        try
        {
            File cmlTemplateDir = ProjectStructure.getCMLTemplatesDir();

            File pasTemplate = new File(cmlTemplateDir, "LeakConductance");

            ChannelMLCellMechanism pas
                = ChannelMLCellMechanism.createFromTemplate(pasTemplate, this.projManager.getCurrentProject());

            File naTemplate = new File(cmlTemplateDir, "NaConductance");

            ChannelMLCellMechanism na
                = ChannelMLCellMechanism.createFromTemplate(naTemplate, this.projManager.getCurrentProject());

            File kTemplate = new File(cmlTemplateDir, "KConductance");

            ChannelMLCellMechanism k
                = ChannelMLCellMechanism.createFromTemplate(kTemplate, this.projManager.getCurrentProject());

            File desTemplate = new File(cmlTemplateDir, "DoubleExpSyn");

            ChannelMLCellMechanism des
                = ChannelMLCellMechanism.createFromTemplate(desTemplate, this.projManager.getCurrentProject());



            pas.initialise(projManager.getCurrentProject(), true);

            String condDensPas = pas.getValue("//@"+ChannelMLConstants.DEFAULT_COND_DENSITY_ATTR);

            logger.logComment("condDensPas: "+condDensPas);

            String unitsUsed = pas.getValue(ChannelMLConstants.getUnitsXPath());

            double condDensDouble = Double.parseDouble(condDensPas);

            if (unitsUsed!=null)
            {
                if (unitsUsed.equals(ChannelMLConstants.SI_UNITS))
                {
                    condDensDouble = UnitConverter.getConductanceDensity(condDensDouble,
                                                                       UnitConverter.GENESIS_SI_UNITS,
                                                                       UnitConverter.NEUROCONSTRUCT_UNITS);
                }
                else if (unitsUsed.equals(ChannelMLConstants.PHYSIOLOGICAL_UNITS))
                {
                    condDensDouble = UnitConverter.getConductanceDensity(condDensDouble,
                                                                       UnitConverter.GENESIS_PHYSIOLOGICAL_UNITS,
                                                                       UnitConverter.NEUROCONSTRUCT_UNITS);
                }
            }


            ChannelMechanism pasChan = new ChannelMechanism(pas.getInstanceName(),
                                                           (float)condDensDouble);


            cell.associateGroupWithChanMech(Section.ALL, pasChan);


            projManager.getCurrentProject().cellMechanismInfo.addCellMechanism(pas);



            na.initialise(projManager.getCurrentProject(), true);

            String condDensNa = na.getValue("//@"+ChannelMLConstants.DEFAULT_COND_DENSITY_ATTR);

            condDensDouble = Double.parseDouble(condDensNa);

            if (unitsUsed!=null)
            {
                if (unitsUsed.equals(ChannelMLConstants.SI_UNITS))
                {
                    condDensDouble = UnitConverter.getConductanceDensity(condDensDouble,
                                                                       UnitConverter.GENESIS_SI_UNITS,
                                                                       UnitConverter.NEUROCONSTRUCT_UNITS);
                }
                else if (unitsUsed.equals(ChannelMLConstants.PHYSIOLOGICAL_UNITS))
                {
                    condDensDouble = UnitConverter.getConductanceDensity(condDensDouble,
                                                                       UnitConverter.GENESIS_PHYSIOLOGICAL_UNITS,
                                                                       UnitConverter.NEUROCONSTRUCT_UNITS);
                }
            }


            ChannelMechanism naChan = new ChannelMechanism(na.getInstanceName(),
                                                           (float)condDensDouble);

            cell.associateGroupWithChanMech(Section.ALL, naChan);


            projManager.getCurrentProject().cellMechanismInfo.addCellMechanism(na);


            k.initialise(projManager.getCurrentProject(), true);

            String condDensK = k.getValue("//@"+ChannelMLConstants.DEFAULT_COND_DENSITY_ATTR);

            condDensDouble = Double.parseDouble(condDensK);

            if (unitsUsed!=null)
            {
                if (unitsUsed.equals(ChannelMLConstants.SI_UNITS))
                {
                    condDensDouble = UnitConverter.getConductanceDensity(condDensDouble,
                                                                       UnitConverter.GENESIS_SI_UNITS,
                                                                       UnitConverter.NEUROCONSTRUCT_UNITS);
                }
                else if (unitsUsed.equals(ChannelMLConstants.PHYSIOLOGICAL_UNITS))
                {
                    condDensDouble = UnitConverter.getConductanceDensity(condDensDouble,
                                                                       UnitConverter.GENESIS_PHYSIOLOGICAL_UNITS,
                                                                       UnitConverter.NEUROCONSTRUCT_UNITS);
                }
            }


            ChannelMechanism kChan = new ChannelMechanism(k.getInstanceName(),
                                                           (float)condDensDouble);

            projManager.getCurrentProject().cellMechanismInfo.addCellMechanism(k);

            cell.associateGroupWithChanMech(Section.ALL, kChan);


            cell.associateGroupWithSpecAxRes(Section.ALL, projManager.getCurrentProject().simulationParameters.getGlobalRa());

            cell.associateGroupWithSpecCap(Section.ALL, projManager.getCurrentProject().simulationParameters.getGlobalCm());

            des.initialise(projManager.getCurrentProject(), true);


            cell.associateGroupWithSynapse(Section.ALL, des.getInstanceName());


            projManager.getCurrentProject().cellMechanismInfo.addCellMechanism(des);

            projManager.getCurrentProject().cellManager.addCellType(cell);


            RectangularBox box = new RectangularBox(0,0,0, 100, 50 ,100);

            projManager.getCurrentProject().regionsInfo.addRow(newRegionName, box, Color.white);

            RandomCellPackingAdapter randAdapter = new RandomCellPackingAdapter();

            int numCellGroups = projManager.getCurrentProject().cellGroupsInfo.getNumberCellGroups();

            projManager.getCurrentProject().cellGroupsInfo.addRow(newCellGroupName,
                                                            newCellName,
                                                            newRegionName,
                                                            Color.red,
                                                            randAdapter,
                                                            10 - numCellGroups);

            projManager.getCurrentProject().simConfigInfo.getDefaultSimConfig().getCellGroups().add(newCellGroupName);


            IClampSettings stim = new IClampSettings(newStim,
                                                     newCellGroupName,
                                                     new AllCells(),
                                                     0,
                                                     20,
                                                     60,
                                                     0.2f,
                                                     false);

            projManager.getCurrentProject().elecInputInfo.addStim(stim);

            projManager.getCurrentProject().simConfigInfo.getDefaultSimConfig().getInputs().add(newStim);


            SimPlot plot = new SimPlot(newPlot,
                                       "SampleGraph",
                                       newCellGroupName,
                                       "*",
                                       "0",
                                       SimPlot.VOLTAGE,
                                       -90,
                                       50,
                                       SimPlot.PLOT_AND_SAVE);

            projManager.getCurrentProject().simPlotInfo.addSimPlot(plot);


            projManager.getCurrentProject().simConfigInfo.getDefaultSimConfig().getPlots().add(newPlot);

            NumericalMethod numMethod = new NumericalMethod();
            numMethod.setMethodNumber(11);
            numMethod.setChanMode(0);
            numMethod.setHsolve(true);


            projManager.getCurrentProject().genesisSettings.setNumMethod(numMethod);


            projManager.getCurrentProject().simulationParameters.setDt(0.02f);

            projManager.getCurrentProject().setProjectDescription(
                             "This is a simple project with a single cell placed randomly in a 3D rectangular box.\n\n"
                             +"Go to tab Generate, press Generate Cell Positions and Connections, and then to visualise"
                             +" the results, go to tab Visualisation and press View, with Latest Generated Positions selected in the drop down box.\n\n"
                                     +"If NEURON or GENESIS are installed, the cell can be simulated via tab Export Network.");

        }
        catch (Exception ex)
        {
            GuiUtils.showErrorMessage(logger, "Error adding sample items to project", ex, this);
            return;
        }

        /*
        logger.logComment("Adding soma simple cell processes...");
        PassiveMembraneProcess pas = new PassiveMembraneProcess();
        pas.setInstanceName(pas.getDefaultInstanceName());
        pas.initialise(this.projManager.getCurrentProject());
        projManager.getCurrentProject().cellProcessInfo.addCellProcess(pas);

        NaChannelProcess na = new NaChannelProcess();
        na.setInstanceName(na.getDefaultInstanceName());
        na.initialise(this.projManager.getCurrentProject());
        projManager.getCurrentProject().cellProcessInfo.addCellProcess(na);


        KChannelProcess k = new KChannelProcess();
        k.setInstanceName(k.getDefaultInstanceName());
        k.initialise(this.projManager.getCurrentProject());
        projManager.getCurrentProject().cellProcessInfo.addCellProcess(k);
*/



    }

    private void cleanseDirectory(File dir)
    {
        if (!dir.isDirectory()) return;
        File[] contents = dir.listFiles();
        for (int i = 0; i < contents.length; i++)
        {
            logger.logComment("Removing: "+ contents[i]);
            if (contents[i].isDirectory())
            {
                cleanseDirectory(contents[i]);

            }
            contents[i].delete();
        }
    }

    /**
     * Carries out all the actions needed when a button or menu item is selected
     * for creating a new 3D region
     *
     */

    private void doNewRegion()
    {
        if (projManager.getCurrentProject() == null)
        {
            logger.logError("No project loaded...");
            return;
        }

        logger.logComment("Creating a new Region...");

        String regionName = null;
        Region newRegion = null;

/*float thickness;
        if (jTextFieldWidth.getText().equals(""))
        {
            jTextFieldWidth.setText(GeneralProperties.getDefaultRegionWidth() + "");
        }

        if (jTextFieldDepth.getText().equals(""))
        {
            jTextFieldDepth.setText(GeneralProperties.getDefaultRegionDepth() + "");
        }

        try
        {
            this.projManager.getCurrentProject().regionsInfo.setRegionWidth(Float.parseFloat(this.jTextFieldWidth.
                getText()));
            this.projManager.getCurrentProject().regionsInfo.setRegionDepth(Float.parseFloat(this.jTextFieldDepth.
                getText()));
        }
        catch (NumberFormatException ex)
        {
            GuiUtils.showErrorMessage(logger,
                                      "Please enter valid numbers into the width and depth fields",
                                      ex, this);
            return;
        }
*/
        int currentNumberRegions = projManager.getCurrentProject().regionsInfo.getRowCount();

        RectangularBox rect = projManager.getCurrentProject().regionsInfo.getRegionEnclosingAllRegions();

        if (rect.getLowestXValue() == rect.getHighestXValue())
        {
            rect.setParameter(RectangularBox.WIDTH_PARAM, GeneralProperties.getDefaultRegionWidth());
        }
        if (rect.getLowestZValue() == rect.getHighestZValue())
        {
            rect.setParameter(RectangularBox.DEPTH_PARAM, GeneralProperties.getDefaultRegionDepth());
        }




        Region suggestedRegion
            = new RectangularBox(rect.getLowestXValue(),
                                 rect.getHighestYValue(),
                                 rect.getLowestZValue(),
                                 rect.getHighestXValue(),
                                 GeneralProperties.getDefaultRegionHeight(),
                                 rect.getHighestZValue());

        //if (suggestedRegion.getl)

        String suggestedName = "Regions_" + (currentNumberRegions + 1);

        RegionsInfoDialog dlg = new RegionsInfoDialog(this,
                                                      suggestedRegion,
                                                      suggestedName);
        Dimension dlgSize = dlg.getPreferredSize();
        Dimension frmSize = getSize();
        Point loc = getLocation();
        dlg.setLocation( (frmSize.width - dlgSize.width) / 2 + loc.x,
                        (frmSize.height - dlgSize.height) / 2 + loc.y);
        dlg.setModal(true);
        dlg.pack();
        dlg.setVisible(true);
        regionName = dlg.getRegionName();
        newRegion = dlg.getFinalRegion();

        if (dlg.cancelled)
        {
            logger.logComment("The action was cancelled...");
            return;
        }

        try
        {
            this.projManager.getCurrentProject().regionsInfo.addRow(regionName, newRegion, Color.white);
        }
        catch (NamingException ex1)
        {
            GuiUtils.showErrorMessage(logger, ex1.getMessage(), ex1, this);
            return;
        }

        jButtonRegionRemove.setEnabled(true);

        this.refreshAll();
    }



    /**
     * Edits selected region
     *
     */

    private void doEditRegion()
    {
        if (projManager.getCurrentProject() == null)
        {
            logger.logError("No project loaded...");
            return;
        }

        int selectedRow = jTable3DRegions.getSelectedRow();

        if (selectedRow < 0)
        {
            logger.logComment("No row selected...");
            return;
        }
        String regionName =
            (String)projManager.getCurrentProject().regionsInfo.getValueAt(selectedRow, RegionsInfo.COL_NUM_REGIONNAME);

        Region region = projManager.getCurrentProject().regionsInfo.getRegionObject(regionName);
        Color colour = projManager.getCurrentProject().regionsInfo.getRegionColour(regionName);


        RegionsInfoDialog dlg = new RegionsInfoDialog(this,
                                                      region,
                                                      regionName);
        Dimension dlgSize = dlg.getPreferredSize();
        Dimension frmSize = getSize();
        Point loc = getLocation();
        dlg.setLocation( (frmSize.width - dlgSize.width) / 2 + loc.x,
                        (frmSize.height - dlgSize.height) / 2 + loc.y);
        dlg.setModal(true);
        dlg.pack();
        dlg.setVisible(true);

        regionName = dlg.getRegionName();
        Region newRegion = dlg.getFinalRegion();

        if (dlg.cancelled)
        {
            logger.logComment("The action was cancelled...");
            return;
        }

        try
        {
            this.projManager.getCurrentProject().regionsInfo.updateRow(regionName, newRegion, colour);
        }
        catch (NamingException ex1)
        {
            GuiUtils.showErrorMessage(logger, ex1.getMessage(), ex1, this);
            return;
        }

        jButtonRegionRemove.setEnabled(true);

        this.refreshAll();
    }


    /**
     * Carries out all the actions needed when a button or menu item is selected
     * for saving the current project
     *
     */
    private void doSave()
    {
        if (projManager.getCurrentProject() == null)
        {
            logger.logError("No project loaded!!");
            return;
        }

        String projectName = this.projManager.getCurrentProject().getProjectName();

        if (projManager.getCurrentProject().getProjectMainDirectory().getParentFile().getAbsolutePath().equals(
                        ProjectStructure.getExamplesDirectory().getAbsolutePath()))
        {
            boolean goOn = GuiUtils.showYesNoMessage(logger, "Note: the project: "+projManager.getCurrentProject().getProjectFile()
                                      +"\nis one of the included example project in neuroConstruct. These are referenced in the documentation and paper.\n"
                                      +"Are you sure you want to save over it?\n\nSelect No to save the project in a different location.", null);

            if (!goOn)
            {
                doSaveAs();
                return;
            }

        }

        logger.logComment("Going to save project: " + projectName);

        try
        {
            projManager.getCurrentProject().saveProject();
            logger.logComment("Project saved");

        }
        catch (Exception ex)
        {
            logger.logError("Error when saving project", ex);
        }
        this.refreshAll();
    }


    public void doReloadLastProject()
    {
        doLoadProject(recentFiles.getFileNames()[0]);
    }

    public void doLoadProject(String projectName)
    {
        logger.logComment(">>>>>>>>>>>   Loading a project at startup...");

        this.jTextAreaProjDescription.setText("\n   Loading project: "+projectName+"...");

        File projFile = new File(projectName);

        if (!projFile.exists())
        {
            GuiUtils.showErrorMessage(logger,
                                      "Cannot find startup file: " + projFile.getAbsolutePath(), null, this);
            return;
        }


        initialisingProject = true;
        try
        {
            projManager.setCurrentProject(Project.loadProject(projFile, this));
        }
        catch (ProjectFileParsingException ex)
        {
            GuiUtils.showErrorMessage(logger, ex.getMessage(), ex, this);
            closeProject();
        }

        recentFiles.addToList(projFile.getAbsolutePath());
        refreshAll();
        enableTableCellEditingFunctionality();

        logger.logComment("<<<<<<<<<<   --------------   *Finished loading the project*   --------------");
        initialisingProject = false;
        createSimulationSummary();
    }

    /**
     * Carries out all the actions needed when a button or menu item is selected
     * for loading a new project
     *
     */
    private void loadProject()
    {
        File defaultDir = null;
        if (projManager.getCurrentProject()!=null)
        {
            defaultDir = projManager.getCurrentProject().getProjectMainDirectory();
        }
        else
        {
            defaultDir = new File(ProjectStructure.getnCProjectsDirectory().getAbsolutePath());
        }

        boolean continueClosing = checkToSave();
        if (!continueClosing) return;
        closeProject();


        Frame frame = (Frame)this;
        JFileChooser chooser = new JFileChooser();
        chooser.setDialogType(JFileChooser.OPEN_DIALOG);

        try
        {
            chooser.setCurrentDirectory(defaultDir);
            logger.logComment("Set Dialog dir to: " + defaultDir);
        }
        catch (Exception ex)
        {
            logger.logError("Problem with default dir setting: " + defaultDir, ex);
        }
        SimpleFileFilter fileFilter
            = new SimpleFileFilter(new String[]{ProjectStructure.getProjectFileExtension()},
                                   "neuroConstruct files. Extension: *"+ProjectStructure.getProjectFileExtension());

        chooser.setFileFilter(fileFilter);

        int retval = chooser.showDialog(frame, null);

        if (retval == JFileChooser.APPROVE_OPTION)
        {
            logger.logComment("User approved...");
            initialisingProject = true;
            try
            {
                logger.logComment(">>>>>>>>  Loading project: "+ chooser.getSelectedFile());
                //projManager.getCurrentProject() = Project.loadProject(chooser.getSelectedFile(), this);
                projManager.doLoadProject(chooser.getSelectedFile());
                logger.logComment("<<<<<<<<  Loaded project: "+ projManager.getCurrentProject().getProjectFileName());
            }
            catch (ProjectFileParsingException ex2)
            {
                GuiUtils.showErrorMessage(logger, ex2.getMessage(), ex2, this);
                initialisingProject = false;
                closeProject();
                return;
            }

            recentFiles.addToList(projManager.getCurrentProject().getProjectFile().getAbsolutePath());
            refreshAll();

            enableTableCellEditingFunctionality();


            initialisingProject = false;
            createSimulationSummary();

            jTabbedPaneMain.setSelectedIndex(0); // main tab...
        }
    }



    /*
     * Carries out all the actions needed when an active project is closed
     */
    private void closeProject()
    {

        logger.logComment("Closing down the project...");
        if (projManager.getCurrentProject() == null)
        {
            logger.logComment("No project loaded to close...");
            return;
        }

        sourceOfCellPosnsInMemory = NO_POSITIONS_LOADED;
        jPanel3DMain.removeAll();

        this.doDestroy3D();


        this.projManager.doCloseProject();

        jTabbedPaneMain.setSelectedIndex(0); // main tab...

        refreshAll();


    }


    /**
     * pops up the create new cell type dialog
     *
     */

    private void doNewCellType()
    {
        if (projManager.getCurrentProject() == null)
        {
            logger.logError("No project loaded...");
            return;
        }

        int currentNumberCellTypes = projManager.getCurrentProject().cellManager.getNumberCellTypes();

        //String morphologyDir = recentFiles.getMyLastMorphologiesDir();

        //if (morphologyDir==null) morphologyDir =(new File(".")).getAbsolutePath(); // pwd...

        NewCellTypeDialog dlg
            = new NewCellTypeDialog(this,
                                    "New Cell Type",
                                    "CellType_" + (currentNumberCellTypes + 1),
                                    projManager.getCurrentProject());

        Dimension dlgSize = dlg.getPreferredSize();
        Dimension frmSize = getSize();
        Point loc = getLocation();
        dlg.setLocation( (frmSize.width - dlgSize.width) / 2 + loc.x,
                        (frmSize.height - dlgSize.height) / 2 + loc.y);
        dlg.setModal(true);
        dlg.pack();
        dlg.setVisible(true);

        if (dlg.createCancelled)
        {
            logger.logComment("They've changed their mind...");
            return;
        }

        Cell newCell = dlg.getChosenCell();


     try
     {
        if (!dlg.wasBasedOnMorphML())
        {
            NumberGenerator initPot = new NumberGenerator();
            initPot.initialiseAsFixedFloatGenerator(projManager.getCurrentProject().simulationParameters.getInitVm());
            newCell.setInitialPotential(initPot);


            newCell.associateGroupWithSpecCap(Section.ALL, projManager.getCurrentProject().simulationParameters.getGlobalCm());

            newCell.associateGroupWithSpecAxRes(Section.ALL, projManager.getCurrentProject().simulationParameters.getGlobalRa());


        }

         projManager.getCurrentProject().cellManager.addCellType(newCell);

     }
     catch (NamingException ex2)
     {
         GuiUtils.showErrorMessage(logger, "Problem with the name of that Cell Type", ex2, this);
     }

     projManager.getCurrentProject().markProjectAsEdited();
     this.refreshAll();


         jComboBoxCellTypes.setSelectedItem(newCell);


    }







    private void doNewAAConn()
    {
        if (projManager.getCurrentProject() == null)
        {
            logger.logError("No project loaded...");
            return;
        }

        int num = projManager.getCurrentProject().volBasedConnsInfo.getNumConns();
        num++;
        String newName = "AAConn_" + num;

        Vector synapticTypes =  projManager.getCurrentProject().cellMechanismInfo.getAllSynMechNames();

        if (synapticTypes.size() == 0)
        {
            GuiUtils.showErrorMessage(logger, "There are no synaptic cell mechanisms in the project.\n"
                                      + "Please add some before creating network connections.", null, this);

            return;
        }



        VolBasedConnDialog dlg = new VolBasedConnDialog(this,
            projManager.getCurrentProject(),
            newName);



        Dimension dlgSize = dlg.getPreferredSize();
        Dimension frmSize = getSize();
        Point loc = getLocation();
        dlg.setLocation( (frmSize.width - dlgSize.width) / 2 + loc.x,
                        (frmSize.height - dlgSize.height) / 2 + loc.y);
        dlg.setModal(true);


        dlg.pack();
        dlg.setVisible(true);


        if (dlg.cancelled)
        {
            logger.logComment("They've changed their mind...");
            return;
        }

        try
        {
            logger.logComment("Trying to add that new net connection...");

            projManager.getCurrentProject().volBasedConnsInfo.addRow(dlg.getAAConnName(),
                                                                         dlg.getSourceCellGroup(),
                                                                         dlg.getTargetCellGroup(),
                                                                         dlg.getSynapticProperties(),
                                                                         dlg.getSourceRegions(),
                                                                         dlg.getConnectivityConditions(),
                                                                         dlg.getAPSpeed(),
                                                                         dlg.getInhomogenousExp().getNiceString());

            refreshTabNetSettings();


           if (this.projManager.getCurrentProject().simConfigInfo.getNumSimConfigs()==1)
           {
               projManager.getCurrentProject().simConfigInfo.getDefaultSimConfig().addNetConn(dlg.getAAConnName());
               logger.logComment("Now netConnss in default SimConfig: "+ projManager.getCurrentProject().simConfigInfo.getDefaultSimConfig().getNetConns());
           }
           else
           {
               GuiUtils.showInfoMessage(logger, "Added Network Connection", "There is more than one Simulation Configurations. To include this Network Connection in one of them, go to tab Generate.", this);
           }

        }
        catch (NamingException ex)
        {
            GuiUtils.showErrorMessage(logger, "Problem adding the Network Connection", ex, this);
            return;
        }
    }

    /**
     * Creates a new connection between 2 cell regions
     *
     */
    private void doNewNetConnection()
    {
        if (projManager.getCurrentProject() == null)
        {
            logger.logError("No project loaded...");
            return;
        }

        int num = projManager.getCurrentProject().morphNetworkConnectionsInfo.getNumSimpleNetConns();
        num++;
        String newName = "NetConn_" + num;

        Vector synapticTypes =  projManager.getCurrentProject().cellMechanismInfo.getAllSynMechNames();

        if (synapticTypes.size() == 0)
        {
            GuiUtils.showErrorMessage(logger, "There are no synaptic cell mechanisms in the project.\n"
                                      + "Please add some before creating network connections.", null, this);

            return;
        }


        NetworkConnectionDialog dlg = new NetworkConnectionDialog(this,
            projManager.getCurrentProject(),
            newName);



        Dimension dlgSize = dlg.getPreferredSize();
        Dimension frmSize = getSize();
        Point loc = getLocation();
        dlg.setLocation( (frmSize.width - dlgSize.width) / 2 + loc.x,
                        (frmSize.height - dlgSize.height) / 2 + loc.y);
        dlg.setModal(true);


        dlg.pack();
        dlg.setVisible(true);


        if (dlg.cancelled)
        {
            logger.logComment("They've changed their mind...");
            return;
        }

        try
        {
            logger.logComment("Trying to add that new net connection...");

            projManager.getCurrentProject().morphNetworkConnectionsInfo.addRow(dlg.getNetworkConnName(),
                dlg.getSourceCellGroup(),
                dlg.getTargetCellGroup(),
                dlg.getSynapticPropsList(),
                dlg.getSearchPattern(),
                dlg.getMaxMinLength(),
                dlg.getConnectivityConditions(),
                dlg.getAPSpeed());

           refreshTabNetSettings();


           if (this.projManager.getCurrentProject().simConfigInfo.getNumSimConfigs()==1)
           {
               projManager.getCurrentProject().simConfigInfo.getDefaultSimConfig().addNetConn(dlg.getNetworkConnName());
               logger.logComment("Now netConnss in default SimConfig: "+ projManager.getCurrentProject().simConfigInfo.getDefaultSimConfig().getNetConns());
           }
           else
           {
               GuiUtils.showInfoMessage(logger, "Added Network Connection", "There is more than one Simulation Configurations. To include this Network Connection in one of them, go to tab Generate.", this);
           }

        }
        catch (NamingException ex)
        {
            GuiUtils.showErrorMessage(logger, "Problem adding the Network Connection", ex, this);
            return;
        }

    }


    /**
     * Creates the 3D representation of a single cell
     *
     */
    void doCreate3DCell(Cell cell)
    {
        if (projManager.getCurrentProject() == null)
        {
            logger.logError("No project loaded...");
            return;
        }

        logger.logComment("Constructing model of one cell: " + cell.getInstanceName());

        logger.logComment("Internal info of cell:");
        logger.logComment(CellTopologyHelper.printDetails(cell, null));

        jPanel3DMain.removeAll();

        jPanel3DMain.setLayout(new BorderLayout());
        Panel panel = new Panel();
        panel.setLayout(new BorderLayout());

        base3DPanel = new OneCell3DPanel(cell, projManager.getCurrentProject(), this);
        //base3DPanel.setViewedObject();

        /////base3DPanel.setDisplayStatus(Base3DPanel.ONE_CELL_DISPLAYED);

        panel.add("Center", base3DPanel);
        jPanel3DMain.add("Center", panel);

        this.validate();

        this.jButton3DDestroy.setEnabled(true);

    }

    /*
     * Creates the 3D representation of the regions
     *
     */
    void doCreate3D(Object selectedObjectToView)
    {
        if (projManager.getCurrentProject() == null)
        {
            logger.logError("No project loaded...");
            return;
        }

        if (selectedObjectToView == null)
        {
            logger.logError("selectedObjectToView is null...");
            return;
        }


        try
        {
            Cell cell = (Cell) selectedObjectToView;
            doCreate3DCell(cell);
            return;
        }
        catch (ClassCastException ex)
        {

        }


        if (selectedObjectToView.equals(LATEST_GENERATED_POSITIONS)
            && (sourceOfCellPosnsInMemory==RELOADED_POSITIONS))
        {

            GuiUtils.showInfoMessage(logger, "Info",
                                     "You've requested to view the \"Latest Generated Positions\", however a previously recorded simulation is in memory.\n"+
                                     "Please press Generate again to create a new set of positions, or select Previous Simulations to select the recorded \n"+
                                     "simulation you wish to view.", this);
            doDestroy3D();
            jComboBoxView3DChoice.setSelectedItem(choice3DChoiceMain);
            return;
        }


        SimulationData simData = null;

        if (selectedObjectToView!=LATEST_GENERATED_POSITIONS)
        {
            File simDataFile = (File)selectedObjectToView;

            try
            {
                simData = new SimulationData(simDataFile);
            }
            catch (SimulationDataException ex1)
            {
                GuiUtils.showErrorMessage(logger, "Error getting the simulation info from "+ simDataFile, ex1, this);
                return;
            }

            logger.logComment("Using set of points from simulation :"+ selectedObjectToView);


                GeneralUtils.timeCheck("Loading cell pos, net conns, etc. from file... ");


            projManager.getCurrentProject().generatedCellPositions.reset();
            try
            {
                projManager.getCurrentProject().generatedCellPositions.loadFromFile(simData.getCellPositionsFile());

                //System.out.println("Positions loaded: "+ projManager.getCurrentProject().generatedCellPositions.getNumberPositionRecords());
            }
            catch (IOException ex2)
            {
                GuiUtils.showErrorMessage(logger,
                                          "Problem loading the cell position data from: " +
                                          simData.getCellPositionsFile(), ex2, this);


                projManager.getCurrentProject().resetGenerated();
                return;
            }


            projManager.getCurrentProject().generatedNetworkConnections.reset();
            try
            {
                projManager.getCurrentProject().generatedNetworkConnections.loadFromFile(simData.getNetConnectionsFile());
            }
            catch (IOException ex2)
            {
                GuiUtils.showErrorMessage(logger,
                                          "Problem loading the net connections data from: " +
                                          simData.getNetConnectionsFile(), ex2, this);

                projManager.getCurrentProject().resetGenerated();
                return;
            }


            projManager.getCurrentProject().generatedElecInputs.reset();
            try
            {
                projManager.getCurrentProject().generatedElecInputs.loadFromFile(simData.getElecInputsFile());
                //System.out.println("Stims loaded: "+ projManager.getCurrentProject().generatedElecInputs);

            }
            catch (IOException ex2)
            {
                GuiUtils.showErrorMessage(logger,
                                          "Problem loading the electrical inputs data from: " +
                                          simData.getElecInputsFile(), ex2, this);

                projManager.getCurrentProject().resetGenerated();
                return;
            }


            Iterator cellGroups = projManager.getCurrentProject().generatedCellPositions.getNamesGeneratedCellGroups();

            while (cellGroups.hasNext())
            {
                String nextCellGroup = (String) cellGroups.next();
                boolean isCellGroup = projManager.getCurrentProject().cellGroupsInfo.isValidCellGroup(nextCellGroup);

                if (!isCellGroup)
                {
                    GuiUtils.showErrorMessage(logger, "The Cell Group " + nextCellGroup + ", as recorded in the simulation data is not a valid Cell Group for this project.\nThis may be due to the project file being altered (e.g Cell Groups changed) after running the simulation.", null, this);

                    projManager.getCurrentProject().resetGenerated();
                    return;
                }
            }

            Iterator netConns = projManager.getCurrentProject().generatedNetworkConnections.getNamesNetConns();

            while (netConns.hasNext())
            {
                String nextNetConn = (String) netConns.next();
                boolean isNetConn = projManager.getCurrentProject().morphNetworkConnectionsInfo.isValidSimpleNetConn(nextNetConn);
                boolean isAAConn = projManager.getCurrentProject().volBasedConnsInfo.isValidAAConn(nextNetConn);

                if (!(isNetConn||isAAConn))
                {
                    GuiUtils.showErrorMessage(logger, "The Network Connection " + nextNetConn + ", as recorded in the simulation data is not a valid Network Connection for this project.\nThis may be due to the project file being altered (e.g Network Connections changed) after running the simulation.", null, this);
                    projManager.getCurrentProject().generatedCellPositions.reset();
                    projManager.getCurrentProject().generatedNetworkConnections.reset();
                    return;
                }
            }
            // No real need to check the inputs, they're just probe positions...



            sourceOfCellPosnsInMemory = RELOADED_POSITIONS;

            logger.logComment("Resetting plot/save info...");
            projManager.getCurrentProject().generatedPlotSaves.reset();

            GeneralUtils.timeCheck("Loaded cell pos, net conns, etc. from file... ");

        }
        else
        {
            if ((projManager.getCurrentProject().cellGroupsInfo.getNumberCellGroups() > 0 &&
                projManager.getCurrentProject().generatedCellPositions.getNumberPositionRecords() == 0)
                || sourceOfCellPosnsInMemory == NO_POSITIONS_LOADED)
            {
                GuiUtils.showErrorMessage(logger,
                                          "Please generate the cell positions before proceeding", null, this);
                return;
            }

        }

        this.jButton3DDestroy.setEnabled(true);

        jPanel3DMain.setLayout(new BorderLayout());
        Panel panel = new Panel();
        panel.setLayout(new BorderLayout());

        if (sourceOfCellPosnsInMemory == GENERATED_POSITIONS || sourceOfCellPosnsInMemory == NETWORKML_POSITIONS )
        {
            // viewing the generated positions
            base3DPanel = new Main3DPanel(this.projManager.getCurrentProject(), null);
        }
        else if (sourceOfCellPosnsInMemory == RELOADED_POSITIONS)
        {
            base3DPanel = new Main3DPanel(this.projManager.getCurrentProject(), simData.getSimulationDirectory());
        }
        panel.add("Center", base3DPanel);
        jPanel3DMain.add("Center", panel);

        this.validate();
    }

    /**
     * Checks if cell posns were reloaded & asks to use these or regenerate
     * @return true to continue, false to stop
     *
     */
    private boolean checkReloadOrRegenerate()
    {
        if (sourceOfCellPosnsInMemory==RELOADED_POSITIONS)
        {
            String warning = "The set of positions, connections and inputs in memory have been reloaded from a saved simulation."
                +"\nThere is not any information on what to plot/save during a simulation associated with this.\n"
                +"To use current information in memory to generate the NEURON network select Continue.\n"
                +"Alternatively, select Regenerate to recreate the network with the appropriate Simulation Configuration.";
                    
           Object[] opts = new Object[]{"Continue", "Regenerate network", "Cancel"};
        
        
           String picked = (String)JOptionPane.showInputDialog(this, 
                                   warning,
                                   "Reuse or regenerate cell positions?", 
                                   JOptionPane.WARNING_MESSAGE , 
                                   null,
                                   opts, 
                                   opts[0]);
        
           
           
           if (picked == null || picked.equals(opts[2])) return false;
           
        
           if (picked.equals(opts[1]))
           {
               jTabbedPaneMain.setSelectedIndex(jTabbedPaneMain.indexOfTab(this.GENERATE_TAB));
               
               this.doGenerate();
               
               return false;
           }
        }
        return true;
    }
    

    /**
     * Creates the *.hoc file for the project
     *
     */
    public void doCreateHoc(int runMode)
    {
        neuronRunMode = runMode;


        if (projManager.getCurrentProject() == null)
        {
            logger.logError("No project loaded...");
            return;
        }

        if (projManager.getCurrentProject().cellGroupsInfo.getNumberCellGroups() == 0 ||
            (projManager.getCurrentProject().cellGroupsInfo.getNumberCellGroups() > 0 &&
            projManager.getCurrentProject().generatedCellPositions.getNumberPositionRecords() == 0))
        {
            GuiUtils.showErrorMessage(logger,
                                      "Please generate the cell positions before proceeding", null, this);
            return;
        }

        boolean cont = this.checkReloadOrRegenerate();
        
        if (!cont) return;


        if (this.jCheckBoxNeuronRandomGen.isSelected())
        {
            Random tempRandom = new Random();
            this.jTextFieldNeuronRandomGen.setText(Math.abs(tempRandom.nextInt())+"");
        }

        long seed = 0;

        //long startTime = System.currentTimeMillis();


        GeneralUtils.timeCheck("Starting generating the hoc code...");



        try
        {
            seed = Long.parseLong(jTextFieldNeuronRandomGen.getText());
        }
        catch (NumberFormatException ex)
        {
            GuiUtils.showErrorMessage(logger, "Please enter a valid integer into the"
                                      +" field for the NEURON random number generator seed", ex, this);
            return;
        }

        refreshSimulationName();


        projManager.getCurrentProject().neuronFileManager.reset();

        try
        {
            MultiRunManager multiRunManager = new MultiRunManager(this.projManager.getCurrentProject(),
                                                  getSelectedSimConfig(),
                                                  projManager.getCurrentProject().simulationParameters.getReference());


            cont = multiRunManager.checkMultiJobSettings();

            if (!cont)
            {
                logger.logComment("User cancelled");
                return;
            }


            projManager.getCurrentProject().neuronFileManager.generateTheNeuronFiles(this.getSelectedSimConfig(), multiRunManager, runMode, seed);
        }
        catch (Exception ex)
        {
            GuiUtils.showErrorMessage(logger, "Problem generating the NEURON files",
                                      ex, this);

            setNeuronRunEnabled(false);
            return;
        }

        if (projManager.getCurrentProject().neuronFileManager.getGeneratedFilenames().size()==0)
        {
            logger.logError("No files generated...");
            setNeuronRunEnabled(false);
            return;
        }

        Vector allModFiles = projManager.getCurrentProject().neuronFileManager.getModFilesToCompile();


        logger.logComment("--- Neuron mod files generated: "+allModFiles);


        GeneralUtils.timeCheck("Neuron files all generated...");

        boolean compileSuccess = true;
        if (allModFiles.size()>0)
        {
            try
            {
                File hocDir = projManager.getCurrentProject().neuronFileManager.getMainHocFile().getParentFile();

                // Note: asking to compile one file will compile the whole dir
                File[] modFiles = hocDir.listFiles(new SimpleFileFilter(new String[]{""}, ""));

                if (modFiles!=null && modFiles.length>0)
                {
                    ProcessManager compileProcess = new ProcessManager(modFiles[0]);

                    logger.logComment("Trying to compile the files in dir: " + modFiles[0].getParentFile());

                    compileSuccess = compileProcess.compileFileWithNeuron();
                }
            }
            catch (Exception ex)
            {
                GuiUtils.showErrorMessage(logger, "Problem compiling the mod files\n"+ex.getMessage(), ex, this);
                setNeuronRunEnabled(false);
                return;
            }

            logger.logComment("Compiled hoc file...");

        }

        if (!compileSuccess)
        {
            logger.logComment("Problem compiling...");

            setNeuronRunEnabled(false);


            return;

        }

        //logger.logComment("Created the hoc code in " +(System.currentTimeMillis() - startTime)+" ms", true);



        setNeuronRunEnabled(true);
        jComboBoxNeuronFiles.removeAllItems();

        String[] types = new String[]{".hoc", ".mod", ".nrn"};
        SimpleFileFilter filter = new SimpleFileFilter(types, "Any NEURON file");


        File[] genFiles = ProjectStructure.getNeuronCodeDir(projManager.getCurrentProject().getProjectMainDirectory()).listFiles(filter);


        for (int i = 0; i < genFiles.length; i++)
        {
            logger.logComment("----    Checking file: "+ genFiles[i]);
            if (!genFiles[i].isDirectory() && !genFiles[i].getName().equals("README"))
            {
                jComboBoxNeuronFiles.addItem(genFiles[i].getName());
            }
        }

        // need this to update list of 3d position files..?
        //this.refreshAll();
        this.refreshTabNeuron();

    }

    private void setNeuronRunEnabled(boolean enabled)
    {

        this.jButtonNeuronRun.setEnabled(enabled);
        this.jButtonNeuronView.setEnabled(enabled);
        this.jComboBoxNeuronFiles.setEnabled(enabled);

    }


    /**
     * Creates the GENESIS files for the project
     *
     */
    protected void doCreateGenesis()
    {
        if (projManager.getCurrentProject() == null)
        {
            logger.logError("No project loaded...");
            return;
        }

        if (projManager.getCurrentProject().cellGroupsInfo.getNumberCellGroups() == 0 ||
            (projManager.getCurrentProject().cellGroupsInfo.getNumberCellGroups() > 0 &&
            projManager.getCurrentProject().generatedCellPositions.getNumberPositionRecords() == 0))
        {
            GuiUtils.showErrorMessage(logger,
                                      "Please generate the cell positions before proceeding", null, this);
            return;
        }


        boolean cont = this.checkReloadOrRegenerate();
        
        if (!cont) return;



        if (this.jCheckBoxGenesisRandomGen.isSelected())
        {
            Random tempRandom = new Random();
            this.jTextFieldGenesisRandomGen.setText(Math.abs(tempRandom.nextInt())+"");
        }

        int seed = 0;
        try
        {
            seed = Integer.parseInt(jTextFieldGenesisRandomGen.getText());
        }
        catch (NumberFormatException ex)
        {
            GuiUtils.showErrorMessage(logger, "Please enter a valid integer into the"
                                      +" field for the GENESIS random number generator seed", ex, this);
            return;
        }


        refreshSimulationName();
        projManager.getCurrentProject().genesisFileManager.reset();

        MorphCompartmentalisation mc = (MorphCompartmentalisation)this.jComboBoxGenesisComps.getSelectedItem();

        MultiRunManager multiRunManager = new MultiRunManager(this.projManager.getCurrentProject(),
                                              getSelectedSimConfig(),
                                              projManager.getCurrentProject().simulationParameters.getReference());

        cont = multiRunManager.checkMultiJobSettings();

        if (!cont)
        {
            logger.logComment("User cancelled");
            return;
        }


        try
        {
            projManager.getCurrentProject().genesisFileManager.generateTheGenesisFiles(this.getSelectedSimConfig(), multiRunManager, mc, seed);
        }
        catch (GenesisException ex)
        {
            GuiUtils.showErrorMessage(logger, "Error when generating the files: " + ex.getMessage(), ex, this);

            this.jButtonGenesisRun.setEnabled(false);
            this.jButtonGenesisView.setEnabled(false);
            this.jComboBoxGenesisFiles.setEnabled(false);

            return;
        }

        this.jButtonGenesisRun.setEnabled(true);
        this.jButtonGenesisView.setEnabled(true);
        this.jComboBoxGenesisFiles.setEnabled(true);

        File[] genFiles = ProjectStructure.getGenesisCodeDir(projManager.getCurrentProject().getProjectMainDirectory()).listFiles();

        jComboBoxGenesisFiles.removeAllItems();

        for (int i = 0; i < genFiles.length; i++)
        {
            if(!genFiles[i].isDirectory() && !genFiles[i].getName().equals("README"))
            {
                jComboBoxGenesisFiles.addItem(genFiles[i].getName());
            }
        }

        // need this to update list of 3d position files...
        refreshAll();

    }

    private void doDestroy3D()
    {

        try
        {
            Main3DPanel main3Dpanel = (Main3DPanel) base3DPanel;
            SimulationRerunFrame simFrame = main3Dpanel.getSimulationFrame();
            simFrame.dispose();
        }
        catch(Exception e)
        {
            // could be class cast or null pointer.
            // either way, ignore..
        }

        jPanel3DMain.removeAll();
        logger.logComment("Removing base3DPanel: " + base3DPanel);
        if (base3DPanel!=null) base3DPanel.destroy3D();
        base3DPanel = null;

        System.gc();
        System.gc();


        this.validate();
        refreshTab3D();
    }



    /**
     * Runs the *.hoc file for the project
     *
     */
    protected void doRunHoc()
    {
        logger.logComment("Running hoc code...");
        if (projManager.getCurrentProject() == null)
        {
            logger.logError("No project loaded...");
            return;
        }

        File genNeuronDir = ProjectStructure.getNeuronCodeDir(projManager.getCurrentProject().
                                                                        getProjectMainDirectory());


        /**
         * Will be the only sim dir if a single run, will be the dir for the actually run neuron code when multiple sims are run
         */
        String primarySimDirName = projManager.getCurrentProject().simulationParameters.getReference();

        //File primarySimDir = ProjectStructure.getDirForSimFiles(primarySimDirName, projManager.getCurrentProject());


        File positionsFile = new File(genNeuronDir, SimulationData.POSITION_DATA_FILE);
        File netConnsFile = new File(genNeuronDir, SimulationData.NETCONN_DATA_FILE);
        File elecInputFile = new File(genNeuronDir, SimulationData.ELEC_INPUT_DATA_FILE);

        try
        {
            projManager.getCurrentProject().generatedCellPositions.saveToFile(positionsFile);
            projManager.getCurrentProject().generatedNetworkConnections.saveToFile(netConnsFile);
            projManager.getCurrentProject().generatedElecInputs.saveToFile(elecInputFile);
        }
        catch (IOException ex)
        {
            GuiUtils.showErrorMessage(logger, "Problem saving generated positions in file: "+ positionsFile.getAbsolutePath(), ex, null);
            return;
        }

        // Saving summary of the simulation params
        try
        {
            SimulationsInfo.recordSimulationSummary(projManager.getCurrentProject(),
                                                    getSelectedSimConfig(), genNeuronDir, "NEURON", null);
        }
        catch (IOException ex2)
        {
            GuiUtils.showErrorMessage(logger, "Error when trying to save a summary of the simulation settings in dir: "
                                      + genNeuronDir +
                                      "\nThere will be less info on this simulation in the previous simulation browser dialog",
                                      ex2, null);
        }


        File[] generatedNeuronFiles = genNeuronDir.listFiles();


        ArrayList<String> simDirsToCreate = projManager.getCurrentProject().neuronFileManager.getGeneratedSimReferences();

        simDirsToCreate.add(primarySimDirName);

        for (String simRef : simDirsToCreate)
        {
            File dirForSimFiles = ProjectStructure.getDirForSimFiles(simRef, projManager.getCurrentProject());

            if (dirForSimFiles.exists())
            {
                SimpleFileFilter sff = new SimpleFileFilter(new String[]
                                                            {".dat"}, null);
                File[] files = dirForSimFiles.listFiles(sff);
                for (int i = 0; i < files.length; i++)
                {
                    files[i].delete();
                }
                logger.logComment("Directory " + dirForSimFiles + " being cleansed");
            }
            else
            {
                GuiUtils.showErrorMessage(logger, "Directory " + dirForSimFiles + " doesn't exist...", null, null);
                return;
            }


            for (int i = 0; i < generatedNeuronFiles.length; i++)
            {
                if (generatedNeuronFiles[i].getName().endsWith(".hoc") ||
                    generatedNeuronFiles[i].getName().endsWith(".mod") ||
                    generatedNeuronFiles[i].getName().endsWith(".dll")||
                    generatedNeuronFiles[i].getName().endsWith(".dat")||
                    generatedNeuronFiles[i].getName().endsWith(".props"))
                {
                    try
                    {
                        //System.out.println("Saving a copy of file: " + generatedNeuronFiles[i]
                        //                  + " to dir: " +
                        //                  dirForSimFiles);

                        GeneralUtils.copyFileIntoDir(generatedNeuronFiles[i],
                                                     dirForSimFiles);
                    }
                    catch (IOException ex)
                    {
                        GuiUtils.showErrorMessage(logger, "Error copying file: " + ex.getMessage(), ex, this);
                        return;
                    }
                }
                else if (generatedNeuronFiles[i].getName().equals("i686") || generatedNeuronFiles[i].getName().equals("x86_64"))
                {
                    File toDir = new File(dirForSimFiles, generatedNeuronFiles[i].getName());
                    toDir.mkdir();
                    logger.logComment("Saving the linux libs from the compiled mods from  of file: " +
                                      generatedNeuronFiles[i]
                                      + " to dir: " +
                                      toDir);

                    try
                    {
                        GeneralUtils.copyDirIntoDir(generatedNeuronFiles[i], toDir, true, true);
                    }
                    catch (IOException ex1)
                    {
                        GuiUtils.showErrorMessage(logger,
                                                  "Error while saving the linux libs from the compiled mods from  of file: " +
                                                  generatedNeuronFiles[i]
                                                  + " to dir: " + dirForSimFiles, ex1, this);

                        return;

                    }

                }

            }

            MatlabOctave.createSimulationLoader(projManager.getCurrentProject(), getSelectedSimConfig(), simRef);

            if (GeneralUtils.isWindowsBasedPlatform() || GeneralUtils.isMacBasedPlatform())
            {
                IgorNeuroMatic.createSimulationLoader(projManager.getCurrentProject(), getSelectedSimConfig(), simRef);
            }
        }

        File simulationDir = ProjectStructure.getDirForSimFiles(projManager.getCurrentProject().simulationParameters.getReference(),
                                                                projManager.getCurrentProject());

        try
        {
            File newMainHocFile = new File(simulationDir,
                               projManager.getCurrentProject().neuronFileManager.getMainHocFile().getName());


            logger.logComment("Going to run file: "+ newMainHocFile);

            projManager.getCurrentProject().neuronFileManager.runNeuronFile(newMainHocFile, neuronRunMode);
        }
        catch (NeuronException ex)
        {
            GuiUtils.showErrorMessage(logger, ex.getMessage(), ex, this);
            return;
        }


        refreshTabNeuron();
        refreshTab3D();

    }


    /**
     * Runs the GENESIS files for the project
     *
     */
    protected void doRunGenesis()
    {
        if (projManager.getCurrentProject() == null)
        {
            logger.logError("No project loaded...");
            return;
        }

        try
        {

            MatlabOctave.createSimulationLoader(projManager.getCurrentProject(), getSelectedSimConfig(), this.jTextFieldSimRef.getText());

            if (GeneralUtils.isWindowsBasedPlatform() || GeneralUtils.isMacBasedPlatform())
            {
                IgorNeuroMatic.createSimulationLoader(projManager.getCurrentProject(), getSelectedSimConfig(),
                                                      this.jTextFieldSimRef.getText());
            }

            projManager.getCurrentProject().genesisFileManager.runGenesisFile();
        }
        catch (GenesisException ex)
        {
            GuiUtils.showErrorMessage(logger, ex.getMessage(), ex, this);
            return;
        }

        //GuiUtils.showInfoMessage(logger,
       //                          "Running simulation in NEURON...",
       //                          "A new simulation entitled "+jTextFieldSimRef.getText()+" is being run in NEURON. Please wait ", this);
        refreshTab3D();

    }


    /**
     * Creates a new Cell Group
     *
     */
    private void doNewCellGroup()
    {
        if (projManager.getCurrentProject() == null)
        {
            logger.logError("No project loaded...");
            return;
        }

        logger.logComment("Creating a new Cell Group...");

        String cellGroupName = null;
        String regionName = null;
        String cellType = null;
        Color cellGroupColour = null;
        CellPackingAdapter adapter = null;
        int priority = 10 - projManager.getCurrentProject().cellGroupsInfo.getNumberCellGroups();

        if (projManager.getCurrentProject().cellManager.getNumberCellTypes() == 0)
        {
            GuiUtils.showErrorMessage(logger,
                                      "Please create one or more Cell Types before proceeding", null, this);
            return;
        }


        if (projManager.getCurrentProject().regionsInfo.getNumberRegions() == 0)
        {
            GuiUtils.showErrorMessage(logger,
                                      "Please create one or more 3D Regions before proceeding", null, this);
            return;
        }
        int numCellGroups = this.projManager.getCurrentProject().cellGroupsInfo.getNumberCellGroups();

        CellGroupDialog dlg = new CellGroupDialog(this, "New Cell Group",
            "CellGroup_" + (numCellGroups + 1), priority, projManager.getCurrentProject());

        Dimension dlgSize = dlg.getPreferredSize();
        Dimension frmSize = getSize();
        Point loc = getLocation();
        dlg.setLocation( (frmSize.width - dlgSize.width) / 2 + loc.x,
                        (frmSize.height - dlgSize.height) / 2 + loc.y);
        dlg.setModal(true);
        dlg.pack();
        dlg.setVisible(true);

        regionName = dlg.getRegionName();
        cellGroupName = dlg.getCellGroupName();
        cellType = dlg.getCellType();
        cellGroupColour = dlg.getCellGroupColour();

        adapter = dlg.getCellPackingAdapter();

        priority = dlg.getPriority();
    ///    enabled = dlg.isCellGroupEnabled();

        if (dlg.cancelled)
        {
            logger.logComment("The action was cancelled...");
            return;
        }
        //this.cellGroupModel.addRow(cellGroupName, cellType, regionName, cellGroupColour, density);
        try
        {
            this.projManager.getCurrentProject().cellGroupsInfo.addRow(cellGroupName,
                                                                       cellType,
                                                                       regionName,
                                                                       cellGroupColour,
                                                                       adapter,
                                                                       priority);
        }
        catch (NamingException ex)
        {
            GuiUtils.showErrorMessage(logger, "Please select another name for the cell group", ex, this);
            return;
        }

        int save = JOptionPane.showConfirmDialog(this, "Would you like the membrane potential of this Cell Group recorded and plotted during simulations?\n"
                                                 +"Note: this can be changed later by altering the Simulation Configuration or deleting\n"
                                                 +"the variable at tab Input and Output", "Save Cell Group voltage?", JOptionPane.YES_NO_OPTION);

        if (save == JOptionPane.YES_OPTION)
        {
            String plotRef = cellGroupName+"_v";
            SimPlot simPlot = new SimPlot(plotRef,
                                          cellGroupName+"_v",
                                          cellGroupName,
                                          "*",
                                          "0",
                                          SimPlot.VOLTAGE,
                                          -90,
                                          50,
                                          SimPlot.PLOT_AND_SAVE);


            projManager.getCurrentProject().simPlotInfo.addSimPlot(simPlot);

            if (this.projManager.getCurrentProject().simConfigInfo.getNumSimConfigs() == 1)
            {
                projManager.getCurrentProject().simConfigInfo.getDefaultSimConfig().addPlot(plotRef);
            }
            else
            {
                GuiUtils.showInfoMessage(logger, "Variable to save/plot added",
                    "There is more than one Simulation Configuration. To specify that this variable to save/plot should be included in one of them (reference: "+plotRef+"), go to tab Generate.", this);
            }

        }

        projManager.getCurrentProject().markProjectAsEdited();
        jButtonCellGroupsDelete.setEnabled(true);

        if (this.projManager.getCurrentProject().simConfigInfo.getNumSimConfigs()==1)
        {
            projManager.getCurrentProject().simConfigInfo.getDefaultSimConfig().addCellGroup(cellGroupName);
            logger.logComment("Now cell groups in default SimConfig: "+ projManager.getCurrentProject().simConfigInfo.getDefaultSimConfig().getCellGroups());
        }
        else
        {
            GuiUtils.showInfoMessage(logger, "Added Cell Group", "There are more than one Simulation Configurations. To add this Cell Group to one of them, go to tab Generate.", this);
        }

        this.refreshTabCellGroupInfo();
    }


    /**
     * Shows the project options dialog box
     *
     * @param tabToSelect which tab to initially set as selected
     * @param mode OptionsFrame.PROJECT_PROPERTIES_MODE etc.
     */

    private void doOptionsPane(String tabToSelect, int mode)
    {

        if (OptionsFrame.isOptionsFrameCurrentlyDisplayed())
        {
            logger.logComment("OptionsFrame is already being displayed...");
            optFrame.toFront();
            return;
        }

        String title = null;
        if (mode==OptionsFrame.PROJECT_PROPERTIES_MODE)
        {
            if (projManager.getCurrentProject() == null)
            {
                logger.logError("No project loaded...");
                return;
            }
            title = "Preferences for project: " + projManager.getCurrentProject().getProjectName();
        }
        else
        {
            title = "General Preferences and Project Default Settings";
        }
        optFrame = new OptionsFrame(this, title, mode);
        optFrame.selectTab(tabToSelect);
        Dimension dlgSize = optFrame.getPreferredSize();
        Dimension frmSize = getSize();
        Point loc = getLocation();
        optFrame.setLocation( (frmSize.width - dlgSize.width) / 2 + loc.x,
                             (frmSize.height - dlgSize.height) / 2 + loc.y);

        optFrame.pack();
        optFrame.setVisible(true);

        if (optFrame.somethingAlteredInProject && mode==OptionsFrame.PROJECT_PROPERTIES_MODE)
        {
            logger.logComment("Something's been altered in the properties...");
            projManager.getCurrentProject().markProjectAsEdited();
        }
        //this.refreshAll();
        optFrame.toFront(); // because the 3D panel is greedy...

    }




    /**
     * Unzips a *.neuro.zip file and opens as a new proj...
     */
    protected void doUnzipProject()
    {
        boolean allOk = checkToSave();

        if (!allOk) return; // i.e. cancelled...

        this.closeProject();


        File defaultDir = ProjectStructure.getnCProjectsDirectory();

        logger.logComment("Unzipping a project...");

        JFileChooser zipFileChooser = new JFileChooser();

        zipFileChooser.setDialogType(JFileChooser.OPEN_DIALOG);
        zipFileChooser.setCurrentDirectory(defaultDir);

        zipFileChooser.setDialogTitle("Open zipped neuroConstruct project");

        SimpleFileFilter fileFilter
            = new SimpleFileFilter(new String[]{ProjectStructure.getProjectZipFileExtension()},
                                   "neuroConstruct zipped projects. Extension: *" +
                                   ProjectStructure.getProjectZipFileExtension());

        zipFileChooser.setFileFilter(fileFilter);

        int retval = zipFileChooser.showDialog(this, "Import project");

        if (retval == JFileChooser.APPROVE_OPTION)
        {
            File chosenZipFile = zipFileChooser.getSelectedFile();

            logger.logComment("File chosen: "+ chosenZipFile);

            if (!chosenZipFile.getName().endsWith(ProjectStructure.getProjectZipFileExtension()))
            {

                GuiUtils.showErrorMessage(logger, "The zip file does not seem to have been generated by "+ ProjectStructure.getProjectFileExtension(), null, this);

                return;

            }

            JFileChooser destDirChooser = new JFileChooser();

            destDirChooser.setFileSelectionMode(JFileChooser.DIRECTORIES_ONLY);

            String newProjName = chosenZipFile.getName().substring(0,
                  chosenZipFile.getName().length() - ProjectStructure.getProjectZipFileExtension().length());

            destDirChooser.setDialogTitle("Please select a directory for the project: "+ newProjName);


            destDirChooser.setCurrentDirectory(ProjectStructure.getnCProjectsDirectory());

            boolean validEmptyDirectory = false;

            while (!validEmptyDirectory)
            {

                retval = destDirChooser.showDialog(this,
                                                   "Create new folder " + newProjName + System.getProperty("file.separator") +
                                                   " for project here");

                if (retval == JFileChooser.APPROVE_OPTION)
                {

                    File chosenParentDir = destDirChooser.getSelectedFile();

                    File projDirToCreate = new File (chosenParentDir.getAbsolutePath()
                                                     + System.getProperty("file.separator")
                                                     + newProjName);

                    logger.logComment("Dir chosen: " + chosenParentDir);

                    if (!projDirToCreate.exists()) projDirToCreate.mkdir();

                    if (projDirToCreate.listFiles().length>0)
                    {
                        GuiUtils.showErrorMessage(logger, "The directory: "+ projDirToCreate +" contains files. Please select a directory with no "+newProjName+" folder.", null, this);

                    }
                    else
                    {
                        validEmptyDirectory = true;
                        try
                        {
                            ZipUtils.unZip(projDirToCreate.getAbsolutePath(), chosenZipFile.getAbsolutePath());

                            String nameOfNewProjectFile = projDirToCreate.getAbsolutePath()
                                                     + System.getProperty("file.separator")
                                                     + newProjName
                                                     + ProjectStructure.getProjectFileExtension();

                            if (!(new File(nameOfNewProjectFile)).exists())
                            {
                                GuiUtils.showErrorMessage(logger, "The expected project file: "+ nameOfNewProjectFile + " was not found!", null, this);
                                projDirToCreate.delete();
                                return;
                            }

                            doLoadProject(nameOfNewProjectFile);
                        }
                        catch (Exception ex)
                        {
                            GuiUtils.showErrorMessage(logger, "Problem extracting the zipped file: " + chosenZipFile + " to "+ projDirToCreate,
                                                      ex, this);
                        }

                    }

                }
                else
                {
                    logger.logComment("User has changed their mind...");
                    return;

                }
            }


        }
        else
        {
            logger.logComment("User has changed their mind...");
            return;
        }

    }


    public void giveUpdate(String update)
    {
        logger.logComment("giveUpdate called with: "+update);

        if (jProgressBarGenerate.getValue()<jProgressBarGenerate.getMaximum())
            this.jProgressBarGenerate.setString(update);

    }


    public void giveGenerationReport(String report, String generatorType, SimConfig simConfig)
    {
        logger.logComment("giveGenerationReport called by: "+ generatorType+ ", report: "+ report);

        if (generatorType.equals(CellPositionGenerator.myGeneratorType))
        {
            jEditorPaneGenerateInfo.setText(report);
            if (projManager.getCurrentProject().generatedCellPositions.getNumberPositionRecords() == 0)
            {
                GuiUtils.showErrorMessage(logger,
                    "No cell positions generated. Please ensure the cell bodies will fit in the selected regions.", null, this);
                return;
            }

            if (report.indexOf("Generation interrupted")>0)
            {
                logger.logComment("It seems the generation of cell positions was interrupted...");
                return;
            }
            projManager.netConnGenerator = new MorphBasedConnGenerator(projManager.getCurrentProject(), this);

            projManager.netConnGenerator.setSimConfig(simConfig);

            projManager.netConnGenerator.start();

        }
        else if (generatorType.equals(MorphBasedConnGenerator.myGeneratorType))
        {

            String currentReport = jEditorPaneGenerateInfo.getText();

            String update = new String(currentReport.substring(0,currentReport.lastIndexOf("</body>")) // as the jEditorPane returns html...
                                       +report);
            jEditorPaneGenerateInfo.setText(update);



            if (report.indexOf("Generation interrupted")>0)
            {
                logger.logComment("It seems the generation of cell positions was interrupted...");
                return;
            }

            projManager.arbourConnectionGenerator = new VolumeBasedConnGenerator(projManager.getCurrentProject(), this);

            projManager.arbourConnectionGenerator.setSimConfig(simConfig);

            projManager.arbourConnectionGenerator.start();



        }

        else if (generatorType.equals(VolumeBasedConnGenerator.myGeneratorType))
        {
            String currentReport = jEditorPaneGenerateInfo.getText();

            String update = new String(currentReport.substring(0,currentReport.lastIndexOf("</body>")) // as the jEditorPane returns html...
                                       +report);

            jEditorPaneGenerateInfo.setText(update);



            if (report.indexOf("Generation interrupted")>0)
            {
                logger.logComment("It seems the generation of cell positions was interrupted...");
                return;
            }


            projManager.elecInputGenerator = new ElecInputGenerator(projManager.getCurrentProject(), this);

            projManager.elecInputGenerator.setSimConfig(simConfig);

            projManager.elecInputGenerator.start();

        }


        else if (generatorType.equals(ElecInputGenerator.myGeneratorType))
        {
            String currentReport = jEditorPaneGenerateInfo.getText();

            String update = new String(currentReport.substring(0, currentReport.lastIndexOf("</body>")) // as the jEditorPane returns html...
                                       + report);
            jEditorPaneGenerateInfo.setText(update);

            if (report.indexOf("Generation interrupted") > 0)
            {
                logger.logComment("It seems the generation of cell positions was interrupted...");
                return;
            }


            projManager.plotSaveGenerator = new PlotSaveGenerator(projManager.getCurrentProject(), this);

            projManager.plotSaveGenerator.setSimConfig(simConfig);

            projManager.plotSaveGenerator.start();


        }

        else if (generatorType.equals(PlotSaveGenerator.myGeneratorType))
        {

            String currentReport = jEditorPaneGenerateInfo.getText();

            String update = new String(currentReport.substring(0, currentReport.lastIndexOf("</body>")) // as the jEditorPane returns html...
                                       + report);

            jEditorPaneGenerateInfo.setText(update);

            this.jButtonGenerateStop.setEnabled(false);

            refreshTabGenerate();
        }
        else
        {
            logger.logComment("Don't know the type of that generation report!!: " + generatorType);
        }



    };


    public void majorStepComplete()
    {
        int currentValue = jProgressBarGenerate.getValue();
        int newVal = currentValue+100;

        logger.logComment("currentValue: "+ currentValue + ", newVal: "+ newVal+ ", max: "+ jProgressBarGenerate.getMaximum());
        jProgressBarGenerate.setValue(newVal);

        if (jProgressBarGenerate.getValue()>=jProgressBarGenerate.getMaximum())
        jProgressBarGenerate.setString("Network generated");
    }




    /**
     * Removes which ever region is selected in table jTable3DRegions
     *
     */

    private void doRemoveRegion()
    {
        if (projManager.getCurrentProject() == null)
        {
            logger.logError("No project loaded...");
            return;
        }

        int selectedRow = jTable3DRegions.getSelectedRow();

        if (selectedRow < 0)
        {
            logger.logComment("No row selected...");
            return;
        }
        String regionName = (String)projManager.getCurrentProject().regionsInfo.getValueAt(selectedRow, RegionsInfo.COL_NUM_REGIONNAME);

        Vector cellGroupsUsingIt = projManager.getCurrentProject().cellGroupsInfo.getCellGroupsInRegion(regionName);

        if (cellGroupsUsingIt.size() > 0)
        {
            StringBuffer errorString = new StringBuffer("The Cell Group");
            if (cellGroupsUsingIt.size() > 1) errorString.append("s: ");
            else errorString.append(": ");

            for (int i = 0; i < cellGroupsUsingIt.size(); i++)
            {
                errorString.append(" " + cellGroupsUsingIt.elementAt(i));
                if (i < cellGroupsUsingIt.size() - 1) errorString.append(", ");
            }
            String buttonText = null;
            if (cellGroupsUsingIt.size() > 1)
            {
                errorString.append(" are in the Region: " + regionName + ". Delete these too?");
                buttonText = "Delete Cell Groups";
            }
            else
            {
                errorString.append(" is in the Region: " + regionName + ". Delete this too?");
                buttonText = "Delete Cell Group";
            }

            Object[] options =
                {buttonText, "Cancel All"};

            JOptionPane option = new JOptionPane(errorString.toString(),
                                                 JOptionPane.DEFAULT_OPTION,
                                                 JOptionPane.WARNING_MESSAGE,
                                                 null,
                                                 options,
                                                 options[0]);

            JDialog dialog = option.createDialog(this, "Warning");
            dialog.setVisible(true);

            Object choice = option.getValue();
            logger.logComment("User has chosen: " + choice);
            if (choice.equals("Cancel All"))
            {
                logger.logComment("User has changed their mind...");
                return;
            }

            for (int i = 0; i < cellGroupsUsingIt.size(); i++)
            {
                String nextCellGroup = (String) cellGroupsUsingIt.elementAt(i);
                logger.logComment("Deleting: " + nextCellGroup);
                //projManager.getCurrentProject().networkConnectionsInfo.deleteNetConn(nextNetConn);

                doRemoveCellGroup(nextCellGroup);

            }
        }

        projManager.getCurrentProject().regionsInfo.deleteRegion(selectedRow);
        logger.logComment("Removed row: " + selectedRow);
        refreshTabRegionsInfo();
    }

    /**
     * Removes whichever cell group is selected in table jTable3DRegions
     *
     */
    private void doRemoveCellGroup(String cellGroupName)
    {
        if (projManager.getCurrentProject() == null)
        {
            logger.logError("No project loaded...");
            return;
        }

        Vector<String> netConnsUsingIt = projManager.getCurrentProject().morphNetworkConnectionsInfo.getNetConnsUsingCellGroup(cellGroupName);
        Vector<String> aaNetConnsUsingIt = projManager.getCurrentProject().volBasedConnsInfo.getAAConnsUsingCellGroup(cellGroupName);
        netConnsUsingIt.addAll(aaNetConnsUsingIt);



        if (netConnsUsingIt.size()>0)
        {
            StringBuffer errorString = new StringBuffer("The Network Connection");
            if (netConnsUsingIt.size()>1) errorString.append("s: ");
                else errorString.append(": ");


            for (int i = 0; i < netConnsUsingIt.size(); i++)
            {
                  errorString.append(" "+ netConnsUsingIt.elementAt(i));
                  if (i<netConnsUsingIt.size()-1) errorString.append(", ");
            }
            String buttonText = null;
            if (netConnsUsingIt.size()>1)
            {
                errorString.append(" use the Cell Group: " + cellGroupName + ". Delete these too?");
                buttonText = "Delete Network Connections";
            }
            else
            {
                errorString.append(" uses the Cell Group: " + cellGroupName + ". Delete this too?");
                buttonText = "Delete Network Connection";
            }


            Object[] options = {buttonText, "Cancel All"};

            JOptionPane option = new JOptionPane(errorString.toString(),
                JOptionPane.DEFAULT_OPTION,
                JOptionPane.WARNING_MESSAGE,
                null,
                options,
                options[0]);

            JDialog dialog = option.createDialog(this, "Warning");
            dialog.setVisible(true);

            Object choice = option.getValue();
            logger.logComment("User has chosen: " + choice);
            if (choice.equals("Cancel All"))
            {
                logger.logComment("User has changed their mind...");
                return;
            }

            for (int i = 0; i < netConnsUsingIt.size(); i++)
            {
                String nextNetConn = (String)netConnsUsingIt.elementAt(i);
                logger.logComment("Deleting: "+ nextNetConn);
                boolean res = projManager.getCurrentProject().morphNetworkConnectionsInfo.deleteNetConn(nextNetConn);
                if (!res) projManager.getCurrentProject().volBasedConnsInfo.deleteConn(nextNetConn);
            }


        }



        projManager.getCurrentProject().cellGroupsInfo.deleteCellGroup(cellGroupName);
        logger.logComment("Removed row: " + cellGroupName);

        projManager.getCurrentProject().simConfigInfo.validateStoredSimConfigs(projManager.getCurrentProject());

        refreshTabCellGroupInfo();
        refreshTabExport(); // due to list of cell groups being shown there...
    }


    /**
     * Edits whichever Cell Group is selected
     *
     */
    private void doEditCellGroup()
    {
        if (projManager.getCurrentProject() == null)
        {
            logger.logError("No project loaded...");
            return;
        }

        int selectedRow = jTableCellGroups.getSelectedRow();

        if (selectedRow < 0)
        {
            logger.logComment("No row selected...");
            return;
        }
        String editCellGroupName = projManager.getCurrentProject().cellGroupsInfo.getCellGroupNameAt(selectedRow);

        CellGroupDialog dlg = new CellGroupDialog(this, "Edit Cell Group",
                                                        editCellGroupName,
                                                        projManager.getCurrentProject().cellGroupsInfo.getCellType(editCellGroupName),
                                                        projManager.getCurrentProject().cellGroupsInfo.getRegionName(editCellGroupName),
                                                        projManager.getCurrentProject().cellGroupsInfo.getColourOfCellGroup(editCellGroupName),
                                                        projManager.getCurrentProject().cellGroupsInfo.getCellPackingAdapter(editCellGroupName),
                                                        projManager.getCurrentProject().cellGroupsInfo.getPriority(editCellGroupName),
                                                        projManager.getCurrentProject());

        Dimension dlgSize = dlg.getPreferredSize();
        Dimension frmSize = getSize();
        Point loc = getLocation();
        dlg.setLocation( (frmSize.width - dlgSize.width) / 2 + loc.x,
                        (frmSize.height - dlgSize.height) / 2 + loc.y);
        dlg.setModal(true);
        dlg.pack();
        dlg.setVisible(true);


        if (dlg.cancelled)
        {
            logger.logComment("The action was cancelled...");
            return;
        }

        projManager.getCurrentProject().cellGroupsInfo.setCellType(editCellGroupName, dlg.getCellType());
        projManager.getCurrentProject().cellGroupsInfo.setRegion(editCellGroupName, dlg.getRegionName());

        projManager.getCurrentProject().cellGroupsInfo.setColourOfCellGroup(editCellGroupName, dlg.getCellGroupColour());
        projManager.getCurrentProject().cellGroupsInfo.setAdapter(editCellGroupName, dlg.getCellPackingAdapter());
        projManager.getCurrentProject().cellGroupsInfo.setPriority(editCellGroupName, dlg.getPriority());

       /// projManager.getCurrentProject().cellGroupsInfo.setCellGroupEnabled(editCellGroupName, dlg.isCellGroupEnabled());

        logger.logComment("Set all alteres parameters");

        projManager.getCurrentProject().markProjectAsEdited();

        this.refreshAll();


    }

    /**
     * Edits whichever network connection is selected in table jTableNetConns
     *
     */
    private void doEditNetConn()
    {
        if (projManager.getCurrentProject() == null)
        {
            logger.logError("No project loaded...");
            return;
        }

        int selectedRow = jTableNetConns.getSelectedRow();

        if (selectedRow < 0)
        {
            logger.logComment("No row selected...");
            return;
        }
        //projManager.getCurrentProject().networkConnectionsInfo.deleteNetConn(selectedRow);

        String editNetConnName = projManager.getCurrentProject().morphNetworkConnectionsInfo.getNetConnNameAt(selectedRow);


        NetworkConnectionDialog dlg
            = new NetworkConnectionDialog(this,
                                         projManager.getCurrentProject(),
                                         editNetConnName,
                                         projManager.getCurrentProject().morphNetworkConnectionsInfo.getSourceCellGroup(editNetConnName),
                                         projManager.getCurrentProject().morphNetworkConnectionsInfo.getTargetCellGroup(editNetConnName),
                                         projManager.getCurrentProject().morphNetworkConnectionsInfo.getSynapseList(editNetConnName),
                                         projManager.getCurrentProject().morphNetworkConnectionsInfo.getSearchPattern(editNetConnName),
                                         /*projManager.getCurrentProject().simpleNetworkConnectionsInfo.getGrowMode(editNetConnName),*/
                                         projManager.getCurrentProject().morphNetworkConnectionsInfo.getMaxMinLength(editNetConnName),
                                         projManager.getCurrentProject().morphNetworkConnectionsInfo.getConnectivityConditions(editNetConnName),
                                         projManager.getCurrentProject().morphNetworkConnectionsInfo.getAPSpeed(editNetConnName));

        Dimension dlgSize = dlg.getPreferredSize();
        Dimension frmSize = getSize();
        Point loc = getLocation();
        dlg.setLocation( (frmSize.width - dlgSize.width) / 2 + loc.x,
                        (frmSize.height - dlgSize.height) / 2 + loc.y);
        dlg.setModal(true);

        dlg.pack();
        dlg.setVisible(true);


        if (dlg.cancelled)
        {
            logger.logComment("They've changed their mind...");
            return;
        }

        projManager.getCurrentProject().morphNetworkConnectionsInfo.setSourceCellGroup(editNetConnName, dlg.getSourceCellGroup());
        projManager.getCurrentProject().morphNetworkConnectionsInfo.setTargetCellGroup(editNetConnName, dlg.getTargetCellGroup());
        projManager.getCurrentProject().morphNetworkConnectionsInfo.setSynapseList(editNetConnName, dlg.getSynapticPropsList());
        projManager.getCurrentProject().morphNetworkConnectionsInfo.setSearchPattern(editNetConnName, dlg.getSearchPattern());
        //projManager.getCurrentProject().simpleNetworkConnectionsInfo.setGrowMode(editNetConnName, dlg.getGrowMode());
        projManager.getCurrentProject().morphNetworkConnectionsInfo.setMaxMinLength(editNetConnName, dlg.getMaxMinLength());
        projManager.getCurrentProject().morphNetworkConnectionsInfo.setConnectivityConditions(editNetConnName, dlg.getConnectivityConditions());
        projManager.getCurrentProject().morphNetworkConnectionsInfo.setAPSpeed(editNetConnName, dlg.getAPSpeed());


       /// this.projManager.getCurrentProject().markProjectAsEdited();
      ///  refreshGeneral();

  }


  /**
   * Edits whichever network connection is selected in table jTableAAConns
   **/

  private void doEditVolConn()
  {
      if (projManager.getCurrentProject() == null)
      {
          logger.logError("No project loaded...");
          return;
      }

      int selectedRow = jTableAAConns.getSelectedRow();

      if (selectedRow < 0)
      {
          logger.logComment("No row selected...");
          return;
      }

      String editNetConnName = projManager.getCurrentProject().volBasedConnsInfo.getConnNameAt(selectedRow);


      VolBasedConnDialog dlg
          = new VolBasedConnDialog(this,
                                       projManager.getCurrentProject(),
                                       editNetConnName,
                                       projManager.getCurrentProject().volBasedConnsInfo.getSourceCellGroup(editNetConnName),
                                       projManager.getCurrentProject().volBasedConnsInfo.getTargetCellGroup(editNetConnName),
                                       projManager.getCurrentProject().volBasedConnsInfo.getSynapseList(editNetConnName),
                                       projManager.getCurrentProject().volBasedConnsInfo.getSourceConnRegions(editNetConnName),
                                       projManager.getCurrentProject().volBasedConnsInfo.getConnectivityConditions(editNetConnName),
                                       projManager.getCurrentProject().volBasedConnsInfo.getAPSpeed(editNetConnName),
                                       projManager.getCurrentProject().volBasedConnsInfo.getInhomogenousExp(editNetConnName));


      Dimension dlgSize = dlg.getPreferredSize();
      Dimension frmSize = getSize();
      Point loc = getLocation();
      dlg.setLocation( (frmSize.width - dlgSize.width) / 2 + loc.x,
                      (frmSize.height - dlgSize.height) / 2 + loc.y);
      dlg.setModal(true);

      dlg.pack();
      dlg.setVisible(true);


      if (dlg.cancelled)
      {
          logger.logComment("They've changed their mind...");
          return;
      }

      projManager.getCurrentProject().volBasedConnsInfo.setSourceCellGroup(editNetConnName, dlg.getSourceCellGroup());
      projManager.getCurrentProject().volBasedConnsInfo.setTargetCellGroup(editNetConnName, dlg.getTargetCellGroup());
      projManager.getCurrentProject().volBasedConnsInfo.setSynapseProperties(editNetConnName, dlg.getSynapticProperties());
      projManager.getCurrentProject().volBasedConnsInfo.setSourceConnRegions(editNetConnName, dlg.getSourceRegions());
      projManager.getCurrentProject().volBasedConnsInfo.setConnectivityConditions(editNetConnName, dlg.getConnectivityConditions());
      projManager.getCurrentProject().volBasedConnsInfo.setAPSpeed(editNetConnName, dlg.getAPSpeed());
      projManager.getCurrentProject().volBasedConnsInfo.setInhomogenousExp(editNetConnName, dlg.getInhomogenousExp().getNiceString());

}

  /**
   * Removes whichever network connection is selected in table jTableNetConns
   *
   */
  private void doRemoveNetConn()
  {
      if (projManager.getCurrentProject() == null)
      {
          logger.logError("No project loaded...");
          return;
      }

      int selectedRow = jTableNetConns.getSelectedRow();

      if (selectedRow < 0)
      {
          logger.logComment("No row selected...");
          return;
      }
      projManager.getCurrentProject().morphNetworkConnectionsInfo.deleteNetConn(selectedRow);
      logger.logComment("Removed row: " + selectedRow);

      projManager.getCurrentProject().simConfigInfo.validateStoredSimConfigs(projManager.getCurrentProject());

      refreshTabNetSettings();
  }


  /**
   * Removes whichever network connection is selected in table jTableAAConns
   **/

  private void doRemoveAANetConn()
  {
      if (projManager.getCurrentProject() == null)
      {
          logger.logError("No project loaded...");
          return;
      }

      int selectedRow = jTableAAConns.getSelectedRow();

      if (selectedRow < 0)
      {
          logger.logComment("No row selected...");
          return;
      }
      projManager.getCurrentProject().volBasedConnsInfo.deleteConn(selectedRow);
      logger.logComment("Removed row: " + selectedRow);

      projManager.getCurrentProject().simConfigInfo.validateStoredSimConfigs(projManager.getCurrentProject());

      refreshTabNetSettings();
  }



      private void doAnalyseCellDensities()
      {
          if (projManager.getCurrentProject() == null)
          {
              logger.logError("No project loaded...");
              return;
          }

          String info =  projManager.getCellDensitiesReport(true);

          logger.logComment("info: "+info);

          SimpleViewer.showString(info,
                        "Information on Cell Densities in project:" + projManager.getCurrentProject().getProjectName(),
                      12,
                      false,
                      true);


      }

      private void doAnalyseNumConns()
      {

        if (projManager.getCurrentProject() == null)
        {
            logger.logError("No project loaded...");
            return;
        }

        String selectedCellGroup = (String)jComboBoxAnalyseCellGroup.getSelectedItem();
        String selectedNetConn = (String)jComboBoxAnalyseNetConn.getSelectedItem();

        if (selectedNetConn.equals(defaultAnalyseNetConnString) ||
            selectedCellGroup.equals(defaultAnalyseCellGroupString))
        {
            GuiUtils.showErrorMessage(logger, "Please select the Network Connection whose Cell Group connectivity you would like to analyse", null, this);
            return;
        }



        boolean isSourceCellGroup = false;
        boolean isTargetCellGroup = false;
        String theOtherCellGroup = null;

        if (projManager.getCurrentProject().morphNetworkConnectionsInfo.isValidSimpleNetConn(selectedNetConn))
        {
            String src = projManager.getCurrentProject().morphNetworkConnectionsInfo.getSourceCellGroup(selectedNetConn);
            String tgt = projManager.getCurrentProject().morphNetworkConnectionsInfo.getTargetCellGroup(selectedNetConn);

            isSourceCellGroup = selectedCellGroup.equals(src);

            isTargetCellGroup = selectedCellGroup.equals(tgt);

             if (isSourceCellGroup) theOtherCellGroup = tgt;
             else theOtherCellGroup = src;
        }
        else if (projManager.getCurrentProject().volBasedConnsInfo.isValidAAConn(selectedNetConn))
        {
            String src = projManager.getCurrentProject().volBasedConnsInfo.getSourceCellGroup(selectedNetConn);
            String tgt = projManager.getCurrentProject().volBasedConnsInfo.getTargetCellGroup(selectedNetConn);

            isSourceCellGroup = selectedCellGroup.equals(src);

            isTargetCellGroup = selectedCellGroup.equals(tgt);

            if (isSourceCellGroup) theOtherCellGroup = tgt;
            else theOtherCellGroup = src;

        }



        if (!isSourceCellGroup && !isTargetCellGroup)
        {
            GuiUtils.showErrorMessage(logger, "The cell group " +
                                      selectedCellGroup
                                      + " is not involved in Network Connection "
                                      + selectedNetConn, null, this);
            return;
        }

        ArrayList<SingleSynapticConnection> netConns = projManager.getCurrentProject().generatedNetworkConnections.getSynapticConnections(selectedNetConn);

        String desc = "No. of conns on "
                                          + selectedCellGroup + " in "
                                          + selectedNetConn;

        PlotterFrame frame = PlotManager.getPlotterFrame(desc);

        DataSet dataSet = new DataSet(selectedCellGroup + " to "+theOtherCellGroup +" conns in "+selectedNetConn,
                                      desc,
            "", "", "Cell index", "Number of conns");

        dataSet.setGraphFormat(PlotCanvas.USE_BARCHART_FOR_PLOT);

        frame.setViewMode(PlotCanvas.INCLUDE_ORIGIN_VIEW);

        int numInCellGroup = projManager.getCurrentProject().generatedCellPositions.getNumberInCellGroup(selectedCellGroup);

        int[] numberConnections = new int[numInCellGroup];
        for (int i = 0; i < numInCellGroup; i++)
        {
            numberConnections[i] = 0;
        }

        for (int i = 0; i < netConns.size(); i++)
        {
            SingleSynapticConnection oneConn = netConns.get(i);

            if (isSourceCellGroup)
            {
                // add 1 for the entry correspondign to the cell number of this single conn...
                  numberConnections[oneConn.sourceEndPoint.cellNumber]++;
            }
            else if (isTargetCellGroup)
            {
                // add 1 for the entry correspondign to the cell number of this single conn...
                  numberConnections[oneConn.targetEndPoint.cellNumber]++;
            }
        }
        for (int i = 0; i < numInCellGroup; i++)
        {
            dataSet.addPoint(i, numberConnections[i]);
        }

        frame.addDataSet(dataSet);
        frame.repaint();



    }


    /**
     * Sets buttons and menu items as enabled when a new project is loaded
     *
     */

    //private void enableItemsDueToprojManager.getCurrentProject()()
   // {




    //}

    /**
     * Refreshes the tabs, frame title, etc. when major changes are made, e.g.
     * when a new project is loaded
     *
     */

    private void refreshAll()
    {
        logger.logComment("----------------    *  Refreshing all  *    ----------------");
        int tabCurrentlySelected = jTabbedPaneMain.getSelectedIndex();

        String nameSelected = jTabbedPaneMain.getTitleAt(tabCurrentlySelected);

        if (!nameSelected.equals(this.VISUALISATION_TAB))
        {
            doDestroy3D();
        }

        if (projManager.getCurrentProject()!=null)
        {
            projManager.getCurrentProject().simConfigInfo.validateStoredSimConfigs(projManager.getCurrentProject());
        }

        this.refreshGeneral();
        this.refreshMenusAndToolbars();
        this.refreshTabProjectInfo();
        this.refreshTabRegionsInfo();
        this.refreshTabCellTypes();
        this.refreshTabCellGroupInfo();
        this.refreshTabCellProcesses();
        this.refreshTabNetSettings();
        this.refreshTabGenerate();
        this.refreshTabExport();
        this.refreshTabNeuron();
        this.refreshTabGenesis();
        this.refreshTab3D();
        logger.logComment("----------------    *  Done refreshing all  *    ----------------");

    }

    /**
     * Refreshes the frame title, etc.
     *
     */

    private void refreshGeneral()
    {
        logger.logComment("> Refreshing the general panel of the application...");
        StringBuffer mainFrameTitle = new StringBuffer();

        if (projManager.getCurrentProject() != null &&
            this.projManager.getCurrentProject().getProjectStatus() != Project.PROJECT_NOT_INITIALISED)
        {
            mainFrameTitle.append("neuroConstruct v"+ GeneralProperties.getVersionNumber());


            try
            {
                mainFrameTitle = mainFrameTitle.append(" - " + projManager.getCurrentProject().getProjectFullFileName());
            }
            catch (NoProjectLoadedException ex)
            {
                logger.logError("Problem getting file name", ex);
                return;
            }
            if (projManager.getCurrentProject().getProjectStatus() == Project.PROJECT_EDITED_NOT_SAVED)
            {
                mainFrameTitle = mainFrameTitle.append("*");
            }
        }
        else
        {
            mainFrameTitle.append("-- No neuroConstruct project loaded --");
        }

        this.setTitle(mainFrameTitle.toString());
    }



    /**
     * Refreshes the menus
     *
     */
    private void refreshMenusAndToolbars()
    {
        logger.logComment("> Refreshing the menus...");

        recentFiles.printDetails();

        String[] recentFileList = recentFiles.getFileNames();

        if(recentFileList.length==0)
        {
            logger.logComment("No recent files found...");
            return;
        }

        jMenuFile.removeAll();
        jMenuFile.add(jMenuItemNewProject);
        jMenuFile.add(jMenuItemFileOpen);

        jMenuFile.add(jMenuItemSaveProject);

        jMenuFile.add(jMenuItemCloseProject);
        jMenuFile.addSeparator();
        jMenuFile.add(jMenuItemCopyProject);
        jMenuFile.add(jMenuItemZipUp);
        jMenuFile.add(jMenuItemUnzipProject);
        jMenuFile.addSeparator();


        for (int i = 0; i < recentFileList.length; i++)
        {
            JMenuItem jMenuRecentFileItem = new JMenuItem();
            jMenuRecentFileItem.setText(recentFileList[i]);
            jMenuFile.add(jMenuRecentFileItem);

            jMenuRecentFileItem.addActionListener(new ActionListener()
            {
                public void actionPerformed(ActionEvent e)
                {
                    jMenuRecentFile_actionPerformed(e);
                }
            });

        }

        jMenuFile.addSeparator();
        jMenuFile.add(jMenuFileExit);

        if (projManager.getCurrentProject() == null ||
            projManager.getCurrentProject().getProjectStatus() == Project.PROJECT_NOT_INITIALISED)
        {
            this.jButtonPreferences.setEnabled(false);
            this.jButtonSaveProject.setEnabled(false);
            this.jButtonValidate.setEnabled(false);
            this.jButtonCloseProject.setEnabled(false);
            this.jMenuItemProjProperties.setEnabled(false);
            this.jMenuItemViewProjSource.setEnabled(false);

        jMenuItemCopyProject.setEnabled(false);
            this.jMenuItemSaveProject.setEnabled(false);
            this.jMenuItemCloseProject.setEnabled(false);
            this.jMenuItemZipUp.setEnabled(false);

            this.jMenuProject.setEnabled(false);

        }
        else
        {
            this.jMenuItemSaveProject.setEnabled(true);
            this.jMenuItemZipUp.setEnabled(true);
            this.jMenuItemUnzipProject.setEnabled(true);

            jMenuItemCopyProject.setEnabled(true);

            this.jMenuItemCloseProject.setEnabled(true);
            this.jMenuItemProjProperties.setEnabled(true);
            this.jMenuItemViewProjSource.setEnabled(true);

            this.jButtonPreferences.setEnabled(true);
            this.jButtonValidate.setEnabled(true);
            this.jButtonSaveProject.setEnabled(true);
            this.jButtonCloseProject.setEnabled(true);

            this.jMenuProject.setEnabled(true);

            if (GeneralProperties.getLogFilePrintToScreenPolicy())
            {
                jButtonToggleConsoleOut.setIcon(imageConsoleOut);
            }
            else
            {
                jButtonToggleConsoleOut.setIcon(imageNoConsoleOut);
            }

        }

        updateConsoleOutState();
    }



    /**
     * Refreshes the tab related to general project info
     *
     */

    private void refreshTabProjectInfo()
    {
        logger.logComment("> Refreshing the Tab for project info...");

        if (initialisingProject && projManager.getCurrentProject() != null)
        {
            this.jTextAreaProjDescription.setText(projManager.getCurrentProject().getProjectDescription());
            jTextAreaProjDescription.setCaretPosition(0);
        }

        if (projManager.getCurrentProject() == null ||
            projManager.getCurrentProject().getProjectStatus() == Project.PROJECT_NOT_INITIALISED)
        {
            this.jPanelCellClicks.removeAll();
            this.jPanelSimConfigClicks.removeAll();
            this.jPanelCellGroupClicks.removeAll();

            logger.logComment("Removed all...");
            jPanelMainInfo.repaint();


            this.jLabelMainNumCells.setEnabled(false);
            this.jLabelSimConfigs.setEnabled(false);
            this.jTextAreaProjDescription.setText(welcomeText);

            this.jLabelProjDescription.setEnabled(false);
            this.jTextFieldProjName.setText("");
            this.jLabelName.setEnabled(false);

            this.jLabelNumCellGroups.setEnabled(false);
            this.jLabelProjFileVersion.setEnabled(false);
            this.jLabelMainLastModified.setEnabled(false);

            this.jTextFieldProjFileVersion.setText("");
            this.jTextFieldMainLastModified.setText("");


            this.jTextAreaProjDescription.setEditable(false);
            Button tempButton = new Button();
            tempButton.setEnabled(false);
            jTextAreaProjDescription.setBackground(tempButton.getBackground());
            jTextAreaProjDescription.setForeground(Color.darkGray);
            this.jScrollPaneProjDesc.setEnabled(false);

        }
        else
        {

            this.jLabelMainNumCells.setEnabled(true);
            this.jLabelSimConfigs.setEnabled(true);
            this.jLabelProjDescription.setEnabled(true);
            this.jLabelName.setEnabled(true);
            this.jLabelNumCellGroups.setEnabled(true);
            this.jLabelProjFileVersion.setEnabled(true);
            this.jLabelMainLastModified.setEnabled(true);


            this.jTextAreaProjDescription.setEditable(true);

            this.jTextFieldProjName.setText(projManager.getCurrentProject().getProjectName());

            //this.jTextFieldNumRegions.setText(this.projManager.getCurrentProject().regionsInfo.getRowCount() + "");
            //this.jTextFieldNumCells.setText(this.projManager.getCurrentProject().cellManager.getNumberCellTypes() + "");

            //this.jTextFieldNumCellGroups.setText(this.projManager.getCurrentProject().cellGroupsInfo.getNumberCellGroups() + "");


            jPanelSimConfigClicks.removeAll();
            for (SimConfig simConfig: projManager.getCurrentProject().simConfigInfo.getAllSimConfigs())
            {
                ClickLink cl = new ClickLink(simConfig.getName(), simConfig.getDescription());

                this.jPanelSimConfigClicks.add(cl);


                cl.addMouseListener(new MouseListener()
                {
                    //String cellGroup = cellGroup;
                    public void mouseClicked(MouseEvent e)
                    {

                        jTabbedPaneMain.setSelectedIndex(jTabbedPaneMain.indexOfTab(GENERATE_TAB));

                        String clicked = e.getComponent().getName();

                        jComboBoxSimConfig.setSelectedItem(clicked);

                        doGenerate();
                    };

                    public void mousePressed(MouseEvent e)
                    {};

                    public void mouseReleased(MouseEvent e)
                    {};

                    public void mouseEntered(MouseEvent e)
                    {};

                    public void mouseExited(MouseEvent e)
                    {};

                });
            }



            jPanelCellClicks.removeAll();
            for (Cell cell: projManager.getCurrentProject().cellManager.getAllCells())
            {
                ClickLink cl = new ClickLink(cell.getInstanceName(), cell.getCellDescription());
                this.jPanelCellClicks.add(cl);


                cl.addMouseListener(new MouseListener()
                {
                    //String cellGroup = cellGroup;
                    public void mouseClicked(MouseEvent e)
                    {

                        jTabbedPaneMain.setSelectedIndex(jTabbedPaneMain.indexOfTab(CELL_TYPES_TAB));

                        String clicked = e.getComponent().getName();

                        Cell clickedCell = projManager.getCurrentProject().cellManager.getCell(clicked);

                        jComboBoxCellTypes.setSelectedItem(clickedCell);

                      //  logger.logComment("Name: "+ clicked, true);
                      //  int index = projManager.getCurrentProject().cellGroupsInfo.getAllCellGroupNames().indexOf(clicked);
                      //  jTableCellGroups.setRowSelectionInterval(index, index);

                        //System.out.println("mouseClicked");
                        //setText("Ouch");
                    };

                    public void mousePressed(MouseEvent e)
                    {};

                    public void mouseReleased(MouseEvent e)
                    {};

                    public void mouseEntered(MouseEvent e)
                    {};

                    public void mouseExited(MouseEvent e)
                    {};

                });
            }

            jPanelCellGroupClicks.removeAll();
            for (String cellGroup: projManager.getCurrentProject().cellGroupsInfo.getAllCellGroupNames())
            {

                ClickLink cl = new ClickLink(cellGroup, "Cell Group: "+cellGroup+"<br>"+
                                             "Cell Type: "+projManager.getCurrentProject().cellGroupsInfo.getCellType(cellGroup));
                this.jPanelCellGroupClicks.add(cl);


                cl.addMouseListener(new MouseListener()
                {
                    //String cellGroup = cellGroup;
                    public void mouseClicked(MouseEvent e)
                    {

                        jTabbedPaneMain.setSelectedIndex(jTabbedPaneMain.indexOfTab(CELL_GROUPS_TAB));

                        String clicked = e.getComponent().getName();
                        logger.logComment("Name: "+ clicked);
                        int index = projManager.getCurrentProject().cellGroupsInfo.getAllCellGroupNames().indexOf(clicked);
                        jTableCellGroups.setRowSelectionInterval(index, index);

                        //System.out.println("mouseClicked");
                        //setText("Ouch");
                    };

                    public void mousePressed(MouseEvent e)
                    {};

                    public void mouseReleased(MouseEvent e)
                    {};

                    public void mouseEntered(MouseEvent e)
                    {};

                    public void mouseExited(MouseEvent e)
                    {};

                });
            }



            this.jTextFieldProjFileVersion.setText(projManager.getCurrentProject().getProjectFileVersion());

            long timeModified = projManager.getCurrentProject().getProjectFile().lastModified();

            SimpleDateFormat formatter = new SimpleDateFormat("H:mm:ss, EEEE MMMM d, yyyy");

            java.util.Date modified = new java.util.Date(timeModified);

            this.jTextFieldMainLastModified.setText(formatter.format(modified));

            this.jTextAreaProjDescription.setEditable(true);

            jTextAreaProjDescription.setBackground((new JTextArea()).getBackground());
            jTextAreaProjDescription.setForeground((new JTextArea()).getForeground());

            this.jScrollPaneProjDesc.setEnabled(true);
        }
    }

    /**
     * Refreshes the tab related to regions info
     *
     */

    private void refreshTabRegionsInfo()
    {
        logger.logComment("> Refreshing the Tab for region info...");
        try
        {
            if (projManager.getCurrentProject() == null || projManager.getCurrentProject().getProjectStatus() == Project.PROJECT_NOT_INITIALISED)
            {
                this.jButtonRegionNew.setEnabled(false);
                this.jButtonRegionsEdit.setEnabled(false);
                jButtonRegionRemove.setEnabled(false);
                //jTextFieldDepth.setText("");
                //jTextFieldWidth.setText("");

                jTable3DRegions.setModel(new RegionsInfo());
            }
            else
            {
                this.jButtonRegionNew.setEnabled(true);

                jTable3DRegions.setModel(projManager.getCurrentProject().regionsInfo);

                if (projManager.getCurrentProject().regionsInfo.getNumberRegions() > 0)
                {
                    jButtonRegionRemove.setEnabled(true);
                    this.jButtonRegionsEdit.setEnabled(true);
                }
                else
                {
                    jButtonRegionRemove.setEnabled(false);
                    this.jButtonRegionsEdit.setEnabled(false);
                }
                /*
                if (jTextFieldWidth.getText().equals(""))
                {
                    jTextFieldWidth.setText(this.projManager.getCurrentProject().regionsInfo.getRegionWidth() + "");
                }
                if (jTextFieldDepth.getText().equals(""))
                {
                    jTextFieldDepth.setText(this.projManager.getCurrentProject().regionsInfo.getRegionDepth() + "");
                }*/

            }
            this.jTable3DRegions.validate();
        }
        catch (java.lang.IllegalStateException ex)
        {
            // This happens when the tab gets updated when a project is closed...
            logger.logComment("Tab being updated whilst project closing down...");
        }
        catch (Exception ex1)
        {
            logger.logError("Error updating Tab Region info", ex1);
        }
    }

    /**
     * Refreshes the tab related to cell group info
     *
     */

    private void refreshTabCellGroupInfo()
    {
        logger.logComment("> Refreshing the Tab for cell group info...");

        if (projManager.getCurrentProject() == null || projManager.getCurrentProject().getProjectStatus() == Project.PROJECT_NOT_INITIALISED)
        {
            this.jButtonCellGroupsNew.setEnabled(false);
            jButtonCellGroupsDelete.setEnabled(false);
            jButtonCellGroupsEdit.setEnabled(false);

            jTableCellGroups.setModel(new CellGroupsInfo());
        }
        else
        {
            this.jButtonCellGroupsNew.setEnabled(true);

            jTableCellGroups.setModel(projManager.getCurrentProject().cellGroupsInfo);

            if (projManager.getCurrentProject().cellGroupsInfo.getNumberCellGroups() > 0)
            {
                jButtonCellGroupsDelete.setEnabled(true);
                jButtonCellGroupsEdit.setEnabled(true);
            }
            else
            {
                jButtonCellGroupsDelete.setEnabled(false);
                jButtonCellGroupsEdit.setEnabled(false);
            }
        }
        this.jTable3DRegions.validate();
    }

    /**
     * Refreshes the tab related to cell info
     *
     */
    private void refreshTabCellTypes()
    {
        logger.logComment("> Refreshing the Tab for cell types...");
        int currentlySelected = jComboBoxCellTypes.getSelectedIndex();
        this.jComboBoxCellTypes.removeAllItems();

        if (projManager.getCurrentProject() == null ||
            projManager.getCurrentProject().getProjectStatus() == Project.PROJECT_NOT_INITIALISED)
        {
            this.jComboBoxCellTypes.addItem(cellComboPrompt);
            this.jButtonCellTypeNew.setEnabled(false);
            this.jButtonCellTypeOtherProject.setEnabled(false);
            this.jComboBoxCellTypes.setEnabled(false);
            this.jButtonCellTypeViewCell.setEnabled(false);
            this.jButtonCellTypeViewCellChans.setEnabled(false);
            this.jButtonCellTypeViewCellInfo.setEnabled(false);
            this.jButtonCellTypeEditDesc.setEnabled(false);
            this.jButtonCellTypeBioPhys.setEnabled(false);
            this.jButtonCellTypeDelete.setEnabled(false);
            this.jButtonCellTypeCompare.setEnabled(false);
            this.jButtonCellTypeCopy.setEnabled(false);
            this.jButtonCellTypesMoveToOrigin.setEnabled(false);
            this.jButtonCellTypesConnect.setEnabled(false);
            this.jButtonCellTypesMakeSimpConn.setEnabled(false);
            return;
        }
        else
        {
            this.jButtonCellTypeNew.setEnabled(true);
            this.jButtonCellTypeOtherProject.setEnabled(true);
            this.jComboBoxCellTypes.setEnabled(true);
            this.jButtonCellTypeViewCell.setEnabled(true);
            this.jButtonCellTypeViewCellChans.setEnabled(true);
            this.jButtonCellTypeViewCellInfo.setEnabled(true);
            this.jButtonCellTypeEditDesc.setEnabled(true);
            this.jButtonCellTypeBioPhys.setEnabled(true);
            this.jButtonCellTypeDelete.setEnabled(true);
            this.jButtonCellTypeCompare.setEnabled(true);
            this.jButtonCellTypeCopy.setEnabled(true);
            this.jButtonCellTypesMoveToOrigin.setEnabled(true);
            //////////this.jButtonCellTypesConnect.setEnabled(true);
            /////////this.jButtonCellTypesMakeSimpConn.setEnabled(true);
        }

        try
        {
            Vector<Cell> cells = this.projManager.getCurrentProject().cellManager.getAllCells();

            GeneralUtils.reorderAlphabetically(cells, true);

            for (Cell cell : cells)
            {
                this.jComboBoxCellTypes.addItem(cell);
            }
        }
        catch (Exception ex)
        {
            logger.logError("Error updating Tab for cell types", ex);
        }
        if (jComboBoxCellTypes.getItemCount() == 0)
        {
            this.jComboBoxCellTypes.addItem(cellComboPrompt);
        }

        if (currentlySelected > 0 && jComboBoxCellTypes.getItemCount()>currentlySelected)
        {
            jComboBoxCellTypes.setSelectedIndex(currentlySelected);
        }
    }



    /**
     * Refreshes the tab related to synapses
     *
     */
    private void refreshTabCellProcesses()
    {
        logger.logComment("> Refreshing the Tab for CellProcesses...");

        if (projManager.getCurrentProject() == null ||
            projManager.getCurrentProject().getProjectStatus() == Project.PROJECT_NOT_INITIALISED)
        {
            this.jButtonMechanismAbstract.setEnabled(false);
            this.jButtonMechanismEdit.setEnabled(false);
            this.jButtonMechanismDelete.setEnabled(false);
            jButtonMechanismFileBased.setEnabled(false);
            jButtonMechanismNewCML.setEnabled(false);
            jButtonMechanismTemplateCML.setEnabled(false);

            jTableMechanisms = new JTable((new CellMechanismInfo()));

        }
        else
        {

            this.jButtonMechanismAbstract.setEnabled(true);
            this.jButtonMechanismEdit.setEnabled(true);
            this.jButtonMechanismDelete.setEnabled(true);
            jButtonMechanismFileBased.setEnabled(true);
            jButtonMechanismNewCML.setEnabled(true);
            jButtonMechanismTemplateCML.setEnabled(true);
            

            jTableMechanisms = new JTable(projManager.getCurrentProject().cellMechanismInfo)
            {
                public String getToolTipText(MouseEvent e) {
                  String tip = null;
                  java.awt.Point p = e.getPoint();
                  int rowIndex = rowAtPoint(p);
                  int colIndex = columnAtPoint(p);
                  
                  //int realColumnIndex = convertColumnIndexToModel(colIndex);


                  //if (realColumnIndex==CellMechanismInfo.COL_NUM_DESC)
                  {
                      String desc = projManager.getCurrentProject().cellMechanismInfo.getCellMechanismAt(rowIndex).getDescription();
                      tip = "<html><b>"+GeneralUtils.replaceAllTokens(desc, "  ", " ")+"</b></html>";
                  }
                  return tip;
                }
            };
            

            jScrollPaneMechanisms.getViewport().add(jTableMechanisms, null);

        }

    }

    /**
     * Refreshes the tab containing the NMODL files
     *

    private void refreshTabNmodl()
    {

        logger.logComment("> Refreshing the Tab for synapses & chan mechs...");

        if (projManager.getCurrentProject() == null ||
            projManager.getCurrentProject().getProjectStatus() == Project.PROJECT_NOT_INITIALISED)
        {
            this.jButtonSynapseAdd.setEnabled(false);
            this.jButtonSynapseEdit.setEnabled(false);
            this.jButtonChanMechAdd.setEnabled(false);
            this.jButtonChanMechEdit.setEnabled(false);

            jTableSynapses.setModel(new SynapticProcessInfo());
            jTableChanMechs.setModel(new ChannelMechanismInfo());
        }
        else
        {
            jTableSynapses.setModel(projManager.getCurrentProject().synapticProcessInfo);
            jTableChanMechs.setModel(projManager.getCurrentProject().channelMechanismInfo);

            projManager.getCurrentProject().synapticProcessInfo.parseDirectory();
            projManager.getCurrentProject().channelMechanismInfo.parseDirectory();

            this.jButtonSynapseAdd.setEnabled(true);
            this.jButtonSynapseEdit.setEnabled(true);
            this.jButtonChanMechAdd.setEnabled(true);
            this.jButtonChanMechEdit.setEnabled(true);

        }

        this.jTableSynapses.validate();
        this.jTableChanMechs.validate();



        TableColumn synNameColumn
            = jTableSynapses.getColumn(jTableSynapses.getColumnName(projManager.getCurrentProject().synapticProcessInfo.
            COL_NUM_NAME));


        synNameColumn.setWidth(10);


    }; */

    /**
     * Refreshes the tab related to network settings
     *
     */
    private void refreshTabNetSettings()
    {
        logger.logComment("> Refreshing the Tab for network settings...");

        if (projManager.getCurrentProject() == null ||
            projManager.getCurrentProject().getProjectStatus() == Project.PROJECT_NOT_INITIALISED)
        {
            jButtonNetConnDelete.setEnabled(false);
            jButtonNetConnEdit.setEnabled(false);
            jButtonNetSetAddNew.setEnabled(false);
            jButtonNetAAAdd.setEnabled(false);
            jButtonNetAADelete.setEnabled(false);
            jButtonNetAAEdit.setEnabled(false);

            jTableNetConns.setModel(new SimpleNetworkConnectionsInfo());
            jTableAAConns.setModel(new ArbourConnectionsInfo());
        }
        else
        {
            jButtonNetSetAddNew.setEnabled(true);

            jButtonNetAAAdd.setEnabled(true);

            jTableNetConns.setModel(projManager.getCurrentProject().morphNetworkConnectionsInfo);
            jTableAAConns.setModel(projManager.getCurrentProject().volBasedConnsInfo);

            logger.logComment("All net conns: "+ projManager.getCurrentProject().morphNetworkConnectionsInfo.getAllSimpleNetConnNames());
            if (projManager.getCurrentProject().morphNetworkConnectionsInfo.getNumSimpleNetConns() > 0)
            {
                jButtonNetConnDelete.setEnabled(true);
                jButtonNetConnEdit.setEnabled(true);
            }
            else
            {
                jButtonNetConnDelete.setEnabled(false);
                jButtonNetConnEdit.setEnabled(false);
            }

            if (projManager.getCurrentProject().volBasedConnsInfo.getNumConns() > 0)
            {
                jButtonNetAAEdit.setEnabled(true);
                jButtonNetAADelete.setEnabled(true);
            }
            else
            {
                jButtonNetAAEdit.setEnabled(false);
                jButtonNetAADelete.setEnabled(false);
            }
        }
    }

    /**
     * Refreshes the tab for generating positions
     *
     */

    private void refreshTabGenerate()
    {
        logger.logComment("> Refreshing the Tab for generating positions...");

        if (projManager.getCurrentProject() == null ||
            projManager.getCurrentProject().getProjectStatus() == Project.PROJECT_NOT_INITIALISED)
        {
            this.jButtonGenerate.setEnabled(false);
            this.jComboBoxSimConfig.setEnabled(false);
            this.jButtonSimConfigEdit.setEnabled(false);
            this.jButtonGenerateSave.setEnabled(false);
            jButtonGenerateLoad.setEnabled(false);
            jCheckBoxGenerateZip.setEnabled(false);
            this.jCheckBoxGenerateExtraNetComments.setEnabled(false);
            this.jEditorPaneGenerateInfo.setText("");
            this.jComboBoxAnalyseCellGroup.setEnabled(false);
            this.jComboBoxAnalyseNetConn.setEnabled(false);
            this.jButtonAnalyseConnLengths.setEnabled(false);
            this.jButtonAnalyseNumConns.setEnabled(false);
            this.jButtonAnalyseCellDensities.setEnabled(false);

            jProgressBarGenerate.setValue(0);
            jProgressBarGenerate.setString("Generation progress");

        }
        else
        {
            this.jButtonGenerate.setEnabled(true);
            this.jComboBoxSimConfig.setEnabled(true);
            this.jButtonSimConfigEdit.setEnabled(true);

            this.jButtonGenerateSave.setEnabled(true);
            jButtonGenerateLoad.setEnabled(true);
            jCheckBoxGenerateZip.setEnabled(true);
            jCheckBoxGenerateExtraNetComments.setEnabled(true);
            ArrayList<String> cellGroupNames = projManager.getCurrentProject().cellGroupsInfo.getAllCellGroupNames();
            Vector<String> netConnNames = projManager.getCurrentProject().morphNetworkConnectionsInfo.getAllSimpleNetConnNames();
            Vector<String> moreNetConnNames = projManager.getCurrentProject().volBasedConnsInfo.getAllAAConnNames();

            netConnNames.addAll(moreNetConnNames);

            int selectedSimConfig = jComboBoxSimConfig.getSelectedIndex();
            jComboBoxSimConfig.removeAllItems();
            ArrayList simConfigs = projManager.getCurrentProject().simConfigInfo.getAllSimConfigNames();
            for (int i = 0; i < simConfigs.size(); i++)
            {
                jComboBoxSimConfig.addItem(simConfigs.get(i));
            }
            if (selectedSimConfig>=0 && jComboBoxSimConfig.getItemCount()>(selectedSimConfig))
                jComboBoxSimConfig.setSelectedIndex(selectedSimConfig);


            if (cellGroupNames.size() > 0 &&
                projManager.getCurrentProject().generatedCellPositions.getNumberPositionRecords() > 0)
            {
                this.jComboBoxAnalyseCellGroup.setEnabled(true);
                this.jButtonAnalyseCellDensities.setEnabled(true);

                if (jComboBoxAnalyseCellGroup.getItemCount()>1 &&
                    jComboBoxAnalyseCellGroup.getSelectedIndex()==0)
                {
                    jComboBoxAnalyseCellGroup.setSelectedIndex(1);
                }

            }
            else
            {
                this.jComboBoxAnalyseCellGroup.setEnabled(false);
                this.jButtonAnalyseCellDensities.setEnabled(false);

            }


            if (netConnNames.size() > 0 &&
                projManager.getCurrentProject().generatedNetworkConnections.getNumberSynapticConnections(GeneratedNetworkConnections.ANY_NETWORK_CONNECTION) > 0)
            {
                this.jComboBoxAnalyseNetConn.setEnabled(true);
                this.jButtonAnalyseConnLengths.setEnabled(true);
                this.jButtonAnalyseNumConns.setEnabled(true);

                jComboBoxAnalyseNetConn.removeAllItems();
                jComboBoxAnalyseNetConn.addItem(defaultAnalyseNetConnString);

                Iterator names = projManager.getCurrentProject().generatedNetworkConnections.getNamesNetConns();

                while (names.hasNext())
                {
                    jComboBoxAnalyseNetConn.addItem(names.next());
                }

                if (jComboBoxAnalyseNetConn.getItemCount()>1 &&
                    jComboBoxAnalyseNetConn.getSelectedIndex()==0)
                {
                    jComboBoxAnalyseNetConn.setSelectedIndex(1);
                }


            }
            else
            {
                this.jComboBoxAnalyseNetConn.setEnabled(false);
                this.jButtonAnalyseConnLengths.setEnabled(false);
                this.jButtonAnalyseNumConns.setEnabled(false);
            }
        }
    }

    /**
     * Refreshes the tab related to export formats
     *
     */
    private void refreshTabExport()
    {
        refreshTabInputOutput();
        refreshTabNeuron();
        refreshTabGenesis();
        refreshTabNeuroML();
    }



    /**
     * Refreshes the tab related to saving parameters, stims, etrc.
     *
     */
    private void refreshTabInputOutput()
    {
        logger.logComment("> Refreshing the Tab for Input and output...");


        if (projManager.getCurrentProject() == null ||
           projManager.getCurrentProject().getProjectStatus() == Project.PROJECT_NOT_INITIALISED)
       {
           jTableSimPlot.setModel(new SimPlotInfo());
           jTableStims.setModel(new ElecInputInfo());


       }
       else
       {
           jTableSimPlot.setModel(projManager.getCurrentProject().simPlotInfo);
           jTableStims.setModel(projManager.getCurrentProject().elecInputInfo);

           TableColumn nextColumn = jTableStims.getColumnModel().getColumn(ElecInputInfo.COL_NUM_CELL_INFO);
           nextColumn.setMinWidth(350);


           TableColumn nextColumn2 = jTableSimPlot.getColumnModel().getColumn(SimPlotInfo.COL_NUM_VALUE);
           nextColumn2.setMinWidth(220);

       }

    }


    /**
     * Refreshes the tab related to GENESIS
     *
     */
    private void refreshTabGenesis()
    {
        logger.logComment("> Refreshing the Tab for GENESIS...");



        if (projManager.getCurrentProject() == null ||
           projManager.getCurrentProject().getProjectStatus() == Project.PROJECT_NOT_INITIALISED)
       {
           jButtonGenesisGenerate.setEnabled(false);
           jButtonGenerateStop.setEnabled(false);
           this.jTextAreaSimConfigDesc.setEnabled(false);
           jButtonGenesisView.setEnabled(false);
           jButtonGenesisNumMethod.setEnabled(false);
           jLabelGenesisNumMethod.setEnabled(false);
           jButtonGenesisRun.setEnabled(false);
           jComboBoxGenesisFiles.setEnabled(false);


           jCheckBoxGenesisSymmetric.setEnabled(false);
           jCheckBoxGenesisComments.setEnabled(false);
           this.jCheckBoxGenesisNoGraphicsMode.setEnabled(false);
           //////////jCheckBoxGenesisVoltPlot.setEnabled(false);
           jCheckBoxGenesisShapePlot.setEnabled(false);

           jLabelGenesisMain.setEnabled(false);
           jRadioButtonGenesisPhy.setEnabled(false);
           jRadioButtonGenesisSI.setEnabled(false);

           //jlabelg

           Button b = new Button();
           b.setEnabled(false);



           jComboBoxGenesisFiles.removeAllItems();
           jComboBoxGenesisFiles.addItem(defaultGenesisFilesText);


           jComboBoxGenesisExtraBlocks.setEnabled(false);
           jComboBoxGenesisExtraBlocks.setSelectedIndex(0);



           this.jTextAreaGenesisBlockDesc.setText("");
           this.jTextAreaGenesisBlock.setText("");
           this.jTextAreaGenesisBlockDesc.setEnabled(false);
           this.jTextAreaGenesisBlock.setEnabled(false);


           this.jTextAreaGenesisBlock.setBackground(b.getBackground());
           this.jTextAreaGenesisBlockDesc.setBackground(b.getBackground());

            this.jLabelGenesisRandomGenDesc.setEnabled(false);
            this.jTextFieldGenesisRandomGen.setEnabled(false);
            this.jCheckBoxGenesisRandomGen.setEnabled(false);
       }
       else
       {
           jButtonGenesisGenerate.setEnabled(true);
           jButtonGenerateStop.setEnabled(true);


           jCheckBoxGenesisSymmetric.setEnabled(true);
           jCheckBoxGenesisComments.setEnabled(true);
           jCheckBoxGenesisNoGraphicsMode.setEnabled(true);
           this.jTextAreaSimConfigDesc.setEnabled(true);

           /////////jCheckBoxGenesisVoltPlot.setEnabled(true);
           jCheckBoxGenesisShapePlot.setEnabled(true);

           jButtonGenesisNumMethod.setEnabled(true);
           jLabelGenesisNumMethod.setEnabled(true);


           jLabelGenesisMain.setEnabled(true);
           jRadioButtonGenesisPhy.setEnabled(true);
           jRadioButtonGenesisSI.setEnabled(true);

           jComboBoxGenesisExtraBlocks.setEnabled(true);

           this.jTextAreaGenesisBlockDesc.setEnabled(true);
           this.jTextAreaGenesisBlock.setEnabled(true);

           this.jLabelGenesisRandomGenDesc.setEnabled(true);
           this.jTextFieldGenesisRandomGen.setEnabled(true);
            this.jCheckBoxGenesisRandomGen.setEnabled(true);


            this.jTextAreaGenesisBlock.setBackground(Color.white);
            this.jTextAreaGenesisBlockDesc.setBackground(Color.white);




           if(initialisingProject)
           {
               ////////////jTextAreaGenesisAfter.setText(projManager.getCurrentProject().genesisSettings.getNativeBlock(ScriptLocation.BEFORE_FINAL_RESET));


               //////////jTextAreaGenesisBefore.setText(projManager.getCurrentProject().genesisSettings.getNativeBlock(ScriptLocation.BEFORE_CELL_CREATION));

               jCheckBoxGenesisSymmetric.setSelected(projManager.getCurrentProject().genesisSettings.isSymmetricCompartments());
               jCheckBoxGenesisComments.setSelected(projManager.getCurrentProject().genesisSettings.isGenerateComments());
               jCheckBoxGenesisNoGraphicsMode.setSelected(!projManager.getCurrentProject().genesisSettings.isGraphicsMode());
               //////////jCheckBoxGenesisVoltPlot.setSelected(projManager.getCurrentProject().genesisSettings.isShowVoltPlot());
               jCheckBoxGenesisShapePlot.setSelected(projManager.getCurrentProject().genesisSettings.isShowShapePlot());

               if (projManager.getCurrentProject().genesisSettings.getUnitSystemToUse() == UnitConverter.GENESIS_PHYSIOLOGICAL_UNITS)
                   jRadioButtonGenesisPhy.setSelected(true);
               if (projManager.getCurrentProject().genesisSettings.getUnitSystemToUse() == UnitConverter.GENESIS_SI_UNITS)
                   jRadioButtonGenesisSI.setSelected(true);

           }

           jLabelGenesisNumMethod.setText(projManager.getCurrentProject().genesisSettings.getNumMethod().toString());


           //if()
       }

    }




    /**
     * Refreshes the tab related to NeuroML
     *
     */
    private void refreshTabNeuroML()
    {
        logger.logComment("> Refreshing the Tab for NeuroML...");

        if (projManager.getCurrentProject() == null ||
           projManager.getCurrentProject().getProjectStatus() == Project.PROJECT_NOT_INITIALISED)
       {
           jButtonNeuroMLExportLevel1.setEnabled(false);
           jButtonNeuroMLExportLevel2.setEnabled(false);
           jButtonNeuroMLExportCellLevel3.setEnabled(false);
           //jButtonNeuroMLExportNetLevel3.setEnabled(false);
           jButtonNeuroMLViewPlain.setEnabled(false);
           jButtonNeuroMLViewFormatted.setEnabled(false);

           jComboBoxNeuroML.removeAllItems();
           jComboBoxNeuroML.setEnabled(false);
       }
       else
       {
           jButtonNeuroMLExportLevel1.setEnabled(true);
           jButtonNeuroMLExportLevel2.setEnabled(true);
           jButtonNeuroMLExportCellLevel3.setEnabled(true);
         //  jButtonNeuroMLExportNetLevel3.setEnabled(true);
           jComboBoxNeuroML.setEnabled(true);

           File morphMLDir = ProjectStructure.getNeuroMLDir(projManager.getCurrentProject().getProjectMainDirectory());

           jComboBoxNeuroML.removeAllItems();
           if (morphMLDir.exists())
           {

               File[] contents = morphMLDir.listFiles();
               for (int i = 0; i < contents.length; i++)
               {
                   if (contents[i].getName().endsWith(".xml")  ||
                       contents[i].getName().endsWith(ProjectStructure.getNeuroMLFileExtension()))
                       jComboBoxNeuroML.addItem(contents[i].getAbsolutePath()
                                                + " ("
                                                + contents[i].length()
                                                + " bytes)");
               }
           }
           if (jComboBoxNeuroML.getItemCount() == 0) jComboBoxNeuroML.addItem(noNeuroMLFilesFound);
           else
           {
               jButtonNeuroMLViewPlain.setEnabled(true);
               jButtonNeuroMLViewFormatted.setEnabled(true);
           }


       }
        logger.logComment("> Done refreshing the Tab for MorphML...");

    }


    /**
     * Refreshes the tab related to NEURON
     *
     */
    private void refreshTabNeuron()
    {
        ///////this.refreshTabNmodl();

        logger.logComment(">>>>>>>>>>>>>>>>>>>>>>>>>>>>>> Refreshing the Tab for Neuron...");

        if (projManager.getCurrentProject()!=null && initialisingProject &&
            projManager.getCurrentProject().getProjectStatus() != Project.PROJECT_NOT_INITIALISED)
        {
            logger.logComment("Putting in the params: " + projManager.getCurrentProject().simulationParameters);

            //projManager.getCurrentProject().neuronSettings.simParams.reference = projManager.getCurrentProject().getProjectName();

            jTextFieldSimRef.setText(this.projManager.getCurrentProject().simulationParameters.getReference());

            jTextFieldSimDefDur.setText(this.projManager.getCurrentProject().simulationParameters.getDuration() + "");
            jTextFieldSimDT.setText(this.projManager.getCurrentProject().simulationParameters.getDt() + "");

            jTextFieldSimulationGlobCm.setText(projManager.getCurrentProject().simulationParameters.getGlobalCm()+"");
            jTextFieldSimulationGlobRm.setText(projManager.getCurrentProject().simulationParameters.getGlobalRm()+"");
            jTextFieldSimulationGlobRa.setText(projManager.getCurrentProject().simulationParameters.getGlobalRa()+"");
            jTextFieldSimulationInitVm.setText(projManager.getCurrentProject().simulationParameters.getInitVm()+"");
            jTextFieldSimulationTemperature.setText(projManager.getCurrentProject().simulationParameters.getTemperature()+"");
            jTextFieldSimulationVLeak.setText(projManager.getCurrentProject().simulationParameters.getGlobalVLeak()+"");


            jCheckBoxSpecifySimRef.setSelected(this.projManager.getCurrentProject().simulationParameters.isSpecifySimName());
            jCheckBoxNeuronSaveHoc.setSelected(this.projManager.getCurrentProject().simulationParameters.isSaveCopyGenSimFiles());

           // refreshSimulationName();
/*
           Hashtable<NativeCodeLocation, String> nativeBlocks = projManager.getCurrentProject().neuronSettings.getNativeBlocks();

           Enumeration<NativeCodeLocation> locsUsed = nativeBlocks.keys();

           while (locsUsed.hasMoreElements())
           {
               NativeCodeLocation nextLoc = locsUsed.nextElement();
               System.out.println("next "+ nextLoc);
               if (nextLoc.equals(NativeCodeLocation.BEFORE_INITIAL))
               {
                   jTextAreaNeuronAfter.setText(nativeBlocks.get(nextLoc));
               }
               else if (nextLoc.equals(NativeCodeLocation.BEFORE_CELL_CREATION))
               {
                   jTextAreaNeuronBefore.setText(nativeBlocks.get(nextLoc));
               }

               //String text = nativeBlocks.get(nextLoc);
           }*/
            ////this.jTextAreaNeuronAfter.setText(projManager.getCurrentProject().neuronSettings.getNativeBlock(NativeCodeLocation.BEFORE_INITIAL));


            //this.jTextAreaNeuronBlock.setText(projManager.getCurrentProject().neuronSettings.getNativeBlock(NativeCodeLocation.BEFORE_CELL_CREATION));

            //this.jTextAreaNeuronBefore.setText(nativeBlocks.get(NativeCodeLocation.BEFORE_CELL_CREATION));

            //System.out.println("nativeBlocks: "+nativeBlocks);
            //System.out.println("nativeBlocks.get("+NativeCodeLocation.BEFORE_INITIAL+"): "+ nativeBlocks.get(NativeCodeLocation.BEFORE_INITIAL));






/*

            if (projManager.getCurrentProject().simulationParameters.getRecordingMode() ==
                SimulationParameters.SIMULATION_NOT_RECORDED)
            {
                jRadioButtonNeuronSimDontRecord.setSelected(true);
            }
            else if (projManager.getCurrentProject().simulationParameters.getRecordingMode() ==
                     SimulationParameters.SIMULATION_RECORD_TO_FILE)
            {
                jRadioButtonNeuronSimSaveToFile.setSelected(true);

            }

            if (projManager.getCurrentProject().simulationParameters.getWhatToRecord() ==
                SimulationParameters.RECORD_ONLY_SOMA)
            {
                jRadioButtonSimSomaOnly.setSelected(true);

            else if (projManager.getCurrentProject().simulationParameters.whatToRecord ==
                     SimulationParameters.RECORD_EVERY_SECTION)
            {
                jRadioButtonSimAllSections.setSelected(true);

            }
            else if (projManager.getCurrentProject().simulationParameters.getWhatToRecord() ==
                     SimulationParameters.RECORD_EVERY_SEGMENT)
            {
                jRadioButtonSimAllSegments.setSelected(true);
            }*/

/*

            if (projManager.getCurrentProject().stimulationSettings != null)
            {

                logger.logComment("There is a stimSettings: "+ projManager.getCurrentProject().stimulationSettings);
                jTextFieldStimDelay.setText(projManager.getCurrentProject().stimulationSettings.delay + "");

                if (projManager.getCurrentProject().stimulationSettings.stimExtent == StimulationSettings.ONE_CELL)
                {
                    jRadioButtonStimOneCell.setSelected(true);
                    jTextFieldStimSomaNum.setText(projManager.getCurrentProject().stimulationSettings.cellNumber + "");
                }
                else
                {
                    jRadioButtonStimPercentage.setSelected(true);
                    jTextFieldStimPercentage.setText(projManager.getCurrentProject().stimulationSettings.percentage + "");
                }
                if (projManager.getCurrentProject().stimulationSettings.type.equals(IClampSettings.type))
                {
                    jRadioButtonStimIClamp.setSelected(true);
                    IClampSettings icStim = (IClampSettings) projManager.getCurrentProject().stimulationSettings;
                    logger.logComment("Adding stuff for IClamp...");
                    jPanelSimParams.removeAll();
                    JPanel tempPanel1 = new JPanel();
                    tempPanel1.setSize(500, 30);
                    tempPanel1.add(new JLabel("Amplitude of stimulation: "));
                    tempPanel1.add(jTextFieldIClampAmplitude);
                    jTextFieldIClampAmplitude.setText(icStim.amplitude + "");
                    jPanelSimParams.add(tempPanel1);
                    JPanel tempPanel2 = new JPanel();
                    tempPanel2.setSize(500, 30);
                    tempPanel2.add(new JLabel("Duration of stimulation: "));
                    tempPanel2.add(jTextFieldIClampDuration);
                    jTextFieldIClampDuration.setText(icStim.duration + "");
                    jPanelSimParams.add(tempPanel2);
                    jPanelSimParams.validate();
                }
                else if (projManager.getCurrentProject().stimulationSettings.type.equals(NetStimSettings.type))
                {
                    jRadioButtonStimNetStim.setSelected(true);
                    NetStimSettings netStim = (NetStimSettings) projManager.getCurrentProject().stimulationSettings;
                    logger.logComment("Adding stuff for NetStim...");
                    jPanelSimParams.removeAll();
                    JPanel tempPanel1 = new JPanel();
                    tempPanel1.setSize(500, 30);
                    tempPanel1.add(new JLabel("Average number of spikes"));
                    tempPanel1.add(jTextFieldNetStimNumber);
                    jTextFieldNetStimNumber.setText(netStim.avgNumSpikes + "");
                    jPanelSimParams.add(tempPanel1);
                    JPanel tempPanel2 = new JPanel();
                    tempPanel2.setSize(500, 30);
                    tempPanel2.add(new JLabel("Noise (0: no noise -> 1: noisiest)"));
                    tempPanel2.add(jTextFieldNetStimNoise);
                    jTextFieldNetStimNoise.setText(netStim.noise + "");
                    jPanelSimParams.add(tempPanel2);
                    jPanelSimParams.validate();
                }
            }
            else
            {
                jRadioButtonStimNone.setSelected(true);
                jPanelSimParams.removeAll();
                jPanelSimParams.validate();
            }
            logger.logComment("done...");*/
        }

        if (projManager.getCurrentProject() == null ||
            projManager.getCurrentProject().getProjectStatus() == Project.PROJECT_NOT_INITIALISED)
        {
            logger.logComment("No project or project not initialised");

            this.jButtonNeuronCreateLocal.setEnabled(false);
            jButtonNeuronCreateCondor.setEnabled(false);
            jButtonNeuronCreateMPI.setEnabled(false);
            this.jButtonNeuronView.setEnabled(false);
            this.jButtonNeuronRun.setEnabled(false);

            this.jCheckBoxNeuronShowShapePlot.setEnabled(false);
            this.jCheckBoxNeuronNoGraphicsMode.setEnabled(false);
            this.jCheckBoxNeuronComments.setEnabled(false);

            this.jCheckBoxNeuronNumInt.setEnabled(false);

            this.jComboBoxNeuronFiles.setEnabled(false);
            this.jTextFieldSimDT.setText("0");
            this.jTextFieldSimDefDur.setText("0");
            this.jTextFieldSimRef.setText("");
            this.jTextFieldSimDT.setEnabled(false);
            this.jTextFieldSimDefDur.setEnabled(false);
            this.jTextFieldSimRef.setEnabled(false);
            this.jRadioButtonNeuronSimDontRecord.setEnabled(false);
            this.jRadioButtonNeuronSimSaveToFile.setEnabled(false);

            jComboBoxNeuronExtraBlocks.setEnabled(false);
            jComboBoxNeuronExtraBlocks.setSelectedIndex(0);



            jLabelSimDefDur.setEnabled(false);
            jLabelSimSummary.setEnabled(false);
            jLabelSimDefDur.setEnabled(false);
            jTextFieldSimDefDur.setEnabled(false);
            jLabelSimDT.setEnabled(false);
            jTextFieldSimDT.setEnabled(false);
            jRadioButtonNeuronSimDontRecord.setEnabled(false);
            jRadioButtonNeuronSimSaveToFile.setEnabled(false);

            jRadioButtonSimAllSegments.setEnabled(false);
            jRadioButtonSimAllSections.setEnabled(false);
            jRadioButtonSimSomaOnly.setEnabled(false);
            jRadioButtonSimSomaOnly.setSelected(true);


            this.jLabelSimSummary.setText("");

            Button b = new Button();
            b.setEnabled(false);

            this.jTextAreaNeuronBlock.setEditable(false);
            this.jTextAreaNeuronBlock.setEnabled(false);
            this.jTextAreaNeuronBlock.setText("");
            this.jTextAreaNeuronBlockDesc.setText("");


            jLabelSimRef.setEnabled(false);
            jTextFieldSimRef.setEnabled(false);
            jCheckBoxSpecifySimRef.setEnabled(false);
            jCheckBoxNeuronSaveHoc.setEnabled(false);


            ////this.jTextAreaNeuronAfter.setText("");
            this.jTextAreaNeuronBlock.setText("");


            ////this.jTextAreaNeuronAfter.setBackground(b.getBackground());
            this.jTextAreaNeuronBlock.setBackground(b.getBackground());
            this.jTextAreaNeuronBlockDesc.setBackground(b.getBackground());

            jComboBoxNeuronFiles.removeAllItems();
            jComboBoxNeuronFiles.addItem(defaultNeuronFilesText);

            this.jTextFieldNeuronRandomGen.setEnabled(false);
            this.jCheckBoxNeuronRandomGen.setEnabled(false);
            this.jTextAreaNeuronBlockDesc.setEnabled(false);
            this.jLabelNeuronRandomGenDesc.setEnabled(false);



            /** @todo make stim buttons uninitialised, etc. */
        }
        else
        {
            logger.logComment("Project is initialised, updating stuff...");
            this.jButtonNeuronCreateLocal.setEnabled(true);
            //////////////////////////////////////////////////jButtonNeuronCreateCondor.setEnabled(true);
            /////////////////////////////////////////////////jButtonNeuronCreatePVM.setEnabled(true);


            if (includeParallelFunc()) jButtonNeuronCreateMPI.setEnabled(true);


            this.jCheckBoxNeuronShowShapePlot.setEnabled(true);
            this.jCheckBoxNeuronNoGraphicsMode.setEnabled(true);
            this.jCheckBoxNeuronComments.setEnabled(true);
            this.jCheckBoxNeuronNumInt.setEnabled(true);


            this.jCheckBoxNeuronShowShapePlot.setSelected(projManager.getCurrentProject().neuronSettings.isShowShapePlot());
            this.jCheckBoxNeuronNoGraphicsMode.setSelected(!projManager.getCurrentProject().neuronSettings.isGraphicsMode());
            this.jCheckBoxNeuronComments.setSelected(projManager.getCurrentProject().neuronSettings.isGenerateComments());
            this.jCheckBoxNeuronNumInt.setSelected(projManager.getCurrentProject().neuronSettings.isVarTimeStep());

            jComboBoxNeuronExtraBlocks.setEnabled(true);


            ////this.jTextAreaNeuronAfter.setEditable(true);
            this.jTextAreaNeuronBlock.setEditable(true);

            ////this.jTextAreaNeuronAfter.setEnabled(true);
            this.jTextAreaNeuronBlock.setEnabled(true);

            ////this.jTextAreaNeuronAfter.setBackground((new JTextArea()).getBackground());
            this.jTextAreaNeuronBlock.setBackground((new JTextArea()).getBackground());

            if (!jComboBoxNeuronExtraBlocks.getSelectedItem().equals(this.neuronBlockPrompt))
            {
                NativeCodeLocation ncl = (NativeCodeLocation) jComboBoxNeuronExtraBlocks.getSelectedItem();
                //System.out.println("NativeCodeLocation selected: " + ncl);
                this.projManager.getCurrentProject().neuronSettings.setNativeBlock(ncl, jTextAreaNeuronBlock.getText());
            }


            ////////jRadioButtonSimAllSegments.setEnabled(true);
           //////// jRadioButtonSimAllSections.setEnabled(true);
            /////jRadioButtonSimSomaOnly.setEnabled(true);


            this.jTextFieldSimDT.setEnabled(true);
            this.jTextFieldSimDefDur.setEnabled(true);
            this.jTextFieldSimRef.setEnabled(true);

            ///////////this.jRadioButtonNeuronSimDontRecord.setEnabled(true);
            ///////////this.jRadioButtonNeuronSimSaveToFile.setEnabled(true);


            jLabelSimDefDur.setEnabled(true);
            jLabelSimSummary.setEnabled(true);
            jLabelSimDefDur.setEnabled(true);
            jTextFieldSimDefDur.setEnabled(true);
            jLabelSimDT.setEnabled(true);
            jTextFieldSimDT.setEnabled(true);
            ////////jRadioButtonNeuronSimDontRecord.setEnabled(true);
            ////////jRadioButtonNeuronSimSaveToFile.setEnabled(true);

            //refreshSimulationName();

            this.jTextFieldNeuronRandomGen.setEnabled(true);
            this.jCheckBoxNeuronRandomGen.setEnabled(true);
            this.jTextAreaNeuronBlockDesc.setEnabled(true);
            this.jLabelNeuronRandomGenDesc.setEnabled(true);


            this.jTextAreaNeuronBlock.setBackground(Color.white);
            this.jTextAreaNeuronBlockDesc.setBackground(Color.white);




            try
            {

                createSimulationSummary();

                jLabelSimRef.setEnabled(true);
                jTextFieldSimRef.setEnabled(true);
                jCheckBoxSpecifySimRef.setEnabled(true);

                if (jCheckBoxSpecifySimRef.isSelected())
                    jTextFieldSimRef.setEnabled(true);
                else
                    jTextFieldSimRef.setEnabled(false);


/*
                if (jRadioButtonNeuronSimDontRecord.isSelected())
                {
                    jLabelNeuronSimRef.setEnabled(false);
                    jTextFieldSimRef.setEnabled(false);
                    jCheckBoxNeuronSpecifySimRef.setEnabled(false);
                    jCheckBoxNeuronSaveHoc.setEnabled(false);
                }
                else if (jRadioButtonNeuronSimSaveToFile.isSelected())
                {
                    jLabelNeuronSimRef.setEnabled(true);
                    //jTextFieldNeuronSimRef.setEnabled(true);
                    jCheckBoxNeuronSpecifySimRef.setEnabled(true);
                    jCheckBoxNeuronSaveHoc.setEnabled(true);

                    if (jCheckBoxNeuronSpecifySimRef.isSelected())
                        jTextFieldSimRef.setEnabled(true);
                    else
                        jTextFieldSimRef.setEnabled(false);
                }*/



            }
            catch (Exception ex)
            {
                logger.logError("Error updating Tab for 3D", ex);
            }
        }
    }


    private void refreshSimulationName()
    {
        /** @todo Replace this, just put here so updating jTextFieldNeuronSimRef
         * wouldn't cause another refresh... */

        boolean currInitialisingState = initialisingProject;
        this.initialisingProject = true;
        if (this.projManager.getCurrentProject().simulationParameters.isSpecifySimName())
        {
            //System.out.println("Holding sim name: in textarea: "+ jTextFieldNeuronSimRef.getText()
           //                    +" stored: "+projManager.getCurrentProject().neuronSettings.simParams.reference);
            jTextFieldSimRef.setText(this.projManager.getCurrentProject().simulationParameters.getReference());
        }
        else
        {
            logger.logComment("Not holding sim name");
            String ref = projManager.getCurrentProject().simulationParameters.getReference();
            //String newRef = null;

            File simDir = ProjectStructure.getSimulationsDir(projManager.getCurrentProject().getProjectMainDirectory());;

            logger.logComment("Sim dir: "+ simDir);

            while ((new File(simDir, ref)).exists())
            {
                logger.logComment("Trying ref: " + ref);
                if (ref.indexOf("_") > 0)
                {
                    try
                    {
                        String numPart = ref.substring(ref.indexOf("_") + 1);
                        int oldNum = Integer.parseInt(numPart);
                        logger.logComment("Old num: " + oldNum);
                        ref = ref.substring(0, ref.indexOf("_")) + "_"+ (oldNum + 1);
                    }
                    catch (NumberFormatException ex)
                    {
                        ref = ref + "_1";
                    }
                }
                else
                {
                    ref = ref + "_1";
                }
            }
            jTextFieldSimRef.setText(ref);
            projManager.getCurrentProject().simulationParameters.setReference(ref);
        }
        this.initialisingProject = currInitialisingState;

    }

    public void applyNew3DSettings()
    {
        projManager.getCurrentProject().markProjectAsEdited();
        this.refreshTab3D();
    }

    /**
     * Refreshes the tab related to cell info
     *
     */
    private void refreshTab3D()
    {
        logger.logComment("> Refreshing the Tab for 3D...");


        if (projManager.getCurrentProject()!=null)
            logger.logComment("Status of project: "+ projManager.getCurrentProject().getProjectStatus());
        else
            logger.logComment("Project is set to null...");

        if (projManager.getCurrentProject()==null ||
            projManager.getCurrentProject().getProjectStatus() == Project.PROJECT_NOT_INITIALISED)
        {
            // no project loaded...
            this.jButton3DView.setEnabled(false);
            this.jButton3DPrevSims.setEnabled(false);
            this.jButton3DDestroy.setEnabled(false);
            this.jComboBoxView3DChoice.setEnabled(false);
            this.jButton3DSettings.setEnabled(false);

            jPanel3DMain.removeAll();
        }
        else
        {
            refreshView3DComboBox();
            Object itemSelected = jComboBoxView3DChoice.getSelectedItem();

            logger.logComment("refreshView3DComboBox itemSelected: "+itemSelected);

            if (projManager.getCurrentProject().regionsInfo.getRowCount() > 0 ||
                projManager.getCurrentProject().cellManager.getNumberCellTypes() >0)
            {
                this.jButton3DView.setEnabled(true);
                this.jButton3DPrevSims.setEnabled(true);

                this.jButton3DSettings.setEnabled(true);
                this.jComboBoxView3DChoice.setEnabled(true);
            }


            // refresh the 3d panel...
            if (base3DPanel != null)
            {
                Transform3D transf = base3DPanel.getLastViewingTransform3D();

                Object previouslyViewedObj = base3DPanel.getViewedObject();

                if (base3DPanel instanceof Main3DPanel)
                {
                    Main3DPanel main3Dpanel = (Main3DPanel) base3DPanel;

                    SimulationRerunFrame simFrame = main3Dpanel.getSimulationFrame();

                    boolean transparency = main3Dpanel.getTransparencySelected();
                    String cellGroup = main3Dpanel.getSelectedCellGroup();
                    //String cellNumber = main3Dpanel.getSelectedCellNumString()+"";

                    logger.logComment("Transparency? "+ transparency);
                    logger.logComment("cellGroup "+ cellGroup);

                    jPanel3DMain.removeAll();

                    this.doCreate3D(previouslyViewedObj);// could be LATEST_GENERATED_POSITIONS or dir

                    base3DPanel.setLastViewingTransform3D(transf);

                    main3Dpanel = (Main3DPanel) base3DPanel;// as its a new object

                    if (simFrame != null) main3Dpanel.setSimulationFrame(simFrame);

                    base3DPanel.refresh3D();


                    main3Dpanel.setTransparencySelected(transparency);
                    main3Dpanel.setSelectedCellGroup(cellGroup);
                    //main3Dpanel.setSelectedCellNumber(cellNumber);

                   // simFrame.setc


                }
                else if (base3DPanel instanceof OneCell3DPanel)
                {
                    jPanel3DMain.removeAll();

                    this.doCreate3DCell((Cell)previouslyViewedObj);
                    base3DPanel.setLastViewingTransform3D(transf);
                    base3DPanel.refresh3D();
                }



            }
        }
    }


    private void refreshView3DComboBox()
    {
        Object itemSelected = jComboBoxView3DChoice.getSelectedItem();
        logger.logComment("refreshView3DComboBox itemSelected: "+itemSelected);
        String stringRepresentation = itemSelected.toString();

        jComboBoxView3DChoice.removeAllItems();

      //  jComboBoxView3DChoice.addItem(choice3DChoiceMain);
        jComboBoxView3DChoice.addItem(LATEST_GENERATED_POSITIONS);
    //    jComboBoxView3DChoice.addItem(choice3DPrevSims);
/*
        File simulationsDir = new File(projManager.getCurrentProject().getProjectMainDirectory(),
                                       GeneralProperties.getDirForSimulations());

        if (!simulationsDir.exists() || !simulationsDir.isDirectory())
        {
            simulationsDir.mkdir();
        }
        File[] childrenDirs = simulationsDir.listFiles();

        logger.logComment("There are " + childrenDirs.length + " files in dir: " +
                          simulationsDir.getAbsolutePath());

        for (int i = 0; i < childrenDirs.length; i++)
        {
            if (childrenDirs[i].isDirectory())
            {
                logger.logComment("Looking at directory: " + childrenDirs[i].getAbsolutePath());

                SimulationData simDataForDir = null;
                try
                {
                    simDataForDir = new SimulationData(childrenDirs[i].getAbsoluteFile());

                    jComboBoxView3DChoice.addItem(simDataForDir);
                    logger.logComment("That's a valid simulation dir...");
                }
                catch (SimulationDataException ex1)
                {
                    logger.logComment("That's not a valid simulation dir...");
                }

            }
        }

        jComboBoxView3DChoice.addItem(choice3DSingleCells);
*/
        try
        {
            Vector<Cell> cells = this.projManager.getCurrentProject().cellManager.getAllCells();

            GeneralUtils.reorderAlphabetically(cells, true);

            for (Cell cell : cells)
            {
                jComboBoxView3DChoice.addItem(cell);
            }
        }
        catch (Exception ex)
        {
            logger.logError("Error updating Tab for 3D", ex);
        }

        for (int i = 0; i < jComboBoxView3DChoice.getItemCount(); i++)
        {
            Object nextItem = jComboBoxView3DChoice.getItemAt(i);
            // do it this way since the SimulationData objects won't be equal between refreshed...
            if (nextItem.toString().equals(stringRepresentation))
                jComboBoxView3DChoice.setSelectedIndex(i);
        }

    }

    /**
     * Asks the user if they wish to save the currently open project
     * @return true if all went well (saved or unsaved), or if there was no
     * project loaded, false if cancelled
     */
    private boolean checkToSave()
    {
        logger.logComment("Checking whether to save..");
        if (projManager.getCurrentProject()==null)
        {
            logger.logComment("No project loaded...");
            return true;
        }
        if (projManager.getCurrentProject().getProjectStatus() == Project.PROJECT_EDITED_NOT_SAVED)
        {
            logger.logComment("Trying to close without saving...");
            String projName = projManager.getCurrentProject().getProjectName();

            SaveBeforeExitDialog dlg = new SaveBeforeExitDialog(this,
                "Save project: " + projName + "?", false);
            Dimension dlgSize = dlg.getPreferredSize();
            Dimension frmSize = getSize();
             Point loc = getLocation();
            dlg.setLocation( (frmSize.width - dlgSize.width) / 2 + loc.x,
                            (frmSize.height - dlgSize.height) / 2 + loc.y);
            dlg.setModal(true);
            dlg.pack();
            dlg.setVisible(true);

            if (dlg.cancelPressed)
            {
                logger.logComment("Chosen to cancel the exit...");
                return false;
            }

            if (dlg.saveTheProject)
            {
                logger.logComment("Chosen to save the project...");
                doSave();
                return true;
            }
        }
        else
        {
            logger.logComment("No need to ask about saving...");

        }
        return true;
    }

    private void addNamedDocumentListner(String tabName, javax.swing.text.JTextComponent comp)
    {
        GeneralComponentListener newListner = new GeneralComponentListener(tabName, this);
        comp.getDocument().addDocumentListener(newListner);
    }

    private void addRadioButtonListner(String tabName, JRadioButton comp)
    {
        GeneralComponentListener newListner = new GeneralComponentListener(tabName, this);
        comp.addItemListener(newListner);
    }

    private void addCheckBoxListner(String tabName, JCheckBox comp)
    {
        GeneralComponentListener newListner = new GeneralComponentListener(tabName, this);
        comp.addItemListener(newListner);
    }



    private class GeneralComponentListener implements DocumentListener, ItemListener
    {
        // This will (hopefully) prevent double listening for events...
        protected boolean listeningEnabled = true;

        String myRef = null;
        ProjectEventListener myEventListner = null;

        GeneralComponentListener(String ref, ProjectEventListener eventListner)
        {
            myRef = ref;
            myEventListner = eventListner;
        }

        private void registerChange()
        {
            if (listeningEnabled) myEventListner.tabUpdated(myRef);
        }

        public void changedUpdate(DocumentEvent e)
        {
            registerChange();
        }

        public void insertUpdate(DocumentEvent e)
        {
            registerChange();
        }

        public void removeUpdate(DocumentEvent e)
        {
            registerChange();
        }

        public void itemStateChanged(ItemEvent e)
        {
            if (e.getStateChange() == ItemEvent.SELECTED)
            {
                registerChange();
            }
            if (e.getItem() instanceof JCheckBox &&
                e.getStateChange() == ItemEvent.DESELECTED)
            {
                registerChange();
            }

        }
    }

    private void createSimulationSummary()
    {
        if (initialisingProject)
        {
            return;
        }
        float simDuration = 0;
        float  simDT = 0;
        try
        {
            simDuration = Float.parseFloat(jTextFieldSimDefDur.getText());
            simDT = Float.parseFloat(jTextFieldSimDT.getText());
        }
        catch(NumberFormatException e)
        {
            logger.logError("NumberFormatException reading ("+jTextFieldSimDefDur.getText()+") and/or ("
                            + jTextFieldSimDT.getText()+")");

            this.jLabelSimSummary.setText("");
            return;
        }

        float numStepsPerMS = (float) (1d / (double)simDT);

        int numStepsTotal = (int) Math.round(simDuration / simDT) + 1;

        StringBuffer comm = new StringBuffer("There will be " + Utils3D.trimDouble(numStepsPerMS, 6) +
                                             " steps per millisecond. The whole simulation will have "
                                             + numStepsTotal);

        comm.append(" steps.");

        this.jLabelSimSummary.setText(comm.toString());

    }

    ///////////////////////////////////////////
    ///
    ///  Functions added by JBuilder...
    ///
    ///////////////////////////////////////////


    //File | Exit action performed
    public void jMenuFileExit_actionPerformed(ActionEvent e)
    {
        boolean proceed = checkToSave();
        if (proceed)
        {
            Logger.getLogger().closeLogFile();
            GeneralProperties.saveToSettingsFile();
            recentFiles.saveToFile();
            System.exit(0);
        }
    }

    //Help | About action performed
    public void jMenuHelpAbout_actionPerformed(ActionEvent e)
    {
        MainFrame_AboutBox dlg = new MainFrame_AboutBox(this);
        Dimension dlgSize = dlg.getPreferredSize();
        Dimension frmSize = getSize();
        Point loc = getLocation();
        dlg.setLocation( (frmSize.width - dlgSize.width) / 2 + loc.x,
                        (frmSize.height - dlgSize.height) / 2 + loc.y);
        dlg.setModal(true);
        dlg.pack();
        dlg.setVisible(true);
    }

    //Help | Units action performed
    public void jMenuItemUsed_actionPerformed(ActionEvent e)
    {
        logger.logComment("Giving info about the units...");

        ImageIcon unitsImage = new ImageIcon(ucl.physiol.neuroconstruct.gui.MainFrame.class.getResource("UpdatedUnits.png"));

        JOptionPane.showMessageDialog(this,
                                      "",
                                      "Units used in neuroConstruct and simulators",
                                      JOptionPane.INFORMATION_MESSAGE,
                                      unitsImage);
    }

    //Overridden so we can exit when window is closed
    protected void processWindowEvent(WindowEvent e)
    {
        //super.processWindowEvent(e);
        if (e.getID() == WindowEvent.WINDOW_CLOSING)
        {
            jMenuFileExit_actionPerformed(null);
        }
        //super.processWindowEvent(e);
    }

    void jButtonOpenProject_actionPerformed(ActionEvent e)
    {
        logger.logComment("Opening a project via a button being pressed...");

        this.loadProject();
    }

    void jButtonSaveProject_actionPerformed(ActionEvent e)
    {
        logger.logComment("Saving the active Project via a button being pressed...");

        this.doSave();

    }

    void jButtonValidate_actionPerformed(ActionEvent e)
    {

        if (projManager.getCurrentProject() == null)
        {
            logger.logError("No project loaded!!");
            return;
        }
        logger.logComment("Validating the project...");

        this.projManager.doValidate(true);

    }

    void jButtonCloseProject_actionPerformed(ActionEvent e)
    {
        logger.logComment("Closing the active Project via a button being pressed...");
        boolean continueClosing = checkToSave();
        if (continueClosing) this.closeProject();
    }

    void jMenuItemSaveProject_actionPerformed(ActionEvent e)
    {
        logger.logComment("Attempting to save via a menu item being selected...");

        this.doSave();

    }

    void jMenuItemNewProject_actionPerformed(ActionEvent e)
    {
        logger.logComment("Attempting to create a new project via a menu item being selected...");
        doNewProject();
    }

    void jButtonNewProject_actionPerformed(ActionEvent e)
    {
        logger.logComment("Attempting to create a new project via a button being pressed...");

        doNewProject();
    }

    /*
        void jTextAreaProjDescription_focusLost(FocusEvent e)
        {
            logger.logComment("Changing the project description");
            projManager.getCurrentProject().setProjectDescription(this.jTextAreaProjDescription.getText());
            this.refreshAll();

        }
     */
    void jMenuItemFileOpen_actionPerformed(ActionEvent e)
    {
        logger.logComment("Opening a project via a menu item being selected...");

        this.loadProject();

    }

    void jButtonCellTypeNew_actionPerformed(ActionEvent e)
    {
        logger.logComment("Creating a new cell type...");
        doNewCellType();

    }


    void jComboBoxSimConfig_itemStateChanged(ItemEvent e)
    {
        SimConfig simConfig = this.getSelectedSimConfig();

        if (simConfig!=null)
        {
            this.jTextAreaSimConfigDesc.setText(simConfig.getDescription());
        }
    }


    void jComboBoxNeuronExtraBlocks_itemStateChanged(ItemEvent e)
    {
        logger.logComment("State change: "+ e.getItem());

        if (e.getItem() == this.neuronBlockPrompt)
        {
            this.jTextAreaNeuronBlock.setText("");

            return;
        }
        if (this.projManager.getCurrentProject()==null) return;

        if (e.getStateChange() == ItemEvent.DESELECTED)
        {
                NativeCodeLocation nclDeselected = (NativeCodeLocation) e.getItem();

                logger.logComment("nclDeselected: " + nclDeselected);

                projManager.getCurrentProject().neuronSettings.setNativeBlock(nclDeselected, this.jTextAreaNeuronBlock.getText());
        }

        else if (e.getStateChange() == ItemEvent.SELECTED)
        {

            NativeCodeLocation ncl = (NativeCodeLocation) e.getItem();

                logger.logComment("ncl selected: " + ncl);

            this.jTextAreaNeuronBlockDesc.setText(ncl.getUsage());

            String text = projManager.getCurrentProject().neuronSettings.getNativeBlock(ncl);

            if (text != null)
            {
                this.jTextAreaNeuronBlock.setText(text);
            }
            else
            {
                logger.logComment("No text found for the location: " + ncl);
                jTextAreaNeuronBlock.setText("");
            }
        }
    }




    void jComboBoxGenesisExtraBlocks_itemStateChanged(ItemEvent e)
    {
        logger.logComment("State change: "+ e.getItem());

        if (e.getItem() == this.genesisBlockPrompt)
        {
            this.jTextAreaGenesisBlock.setText("");

            return;
        }
        if (this.projManager.getCurrentProject()==null) return;

        if (e.getStateChange() == ItemEvent.DESELECTED)
        {
                ScriptLocation nclDeselected = (ScriptLocation) e.getItem();

                logger.logComment("nclDeselected: " + nclDeselected);

                projManager.getCurrentProject().genesisSettings.setNativeBlock(nclDeselected, this.jTextAreaGenesisBlock.getText());
        }

        else if (e.getStateChange() == ItemEvent.SELECTED)
        {

            ScriptLocation ncl = (ScriptLocation) e.getItem();

                logger.logComment("ncl selected: " + ncl);

            this.jTextAreaGenesisBlockDesc.setText(ncl.getUsage());

            String text = projManager.getCurrentProject().genesisSettings.getNativeBlock(ncl);

            if (text != null)
            {
                this.jTextAreaGenesisBlock.setText(text);
            }
            else
            {
                logger.logComment("No text found for the location: " + ncl);
                jTextAreaGenesisBlock.setText("");
            }
        }
    }

    void jComboBoxNeuroMLComps_itemStateChanged(ItemEvent e)
    {
        MorphCompartmentalisation mc = (MorphCompartmentalisation)jComboBoxNeuroMLComps.getSelectedItem();

        this.jTextAreaNeuroMLCompsDesc.setText(mc.getDescription());
    }

    void jComboBoxGenesisComps_itemStateChanged(ItemEvent e)
    {
        MorphCompartmentalisation mc = (MorphCompartmentalisation)this.jComboBoxGenesisComps.getSelectedItem();

        this.jTextAreaGenesisCompsDesc.setText(mc.getDescription());
    }



    void jComboBoxCellTypes_itemStateChanged(ItemEvent e)
    {
        if (e.getItem() == this.cellComboPrompt)
        {
            jEditorPaneCellTypeInfo.setText("");

            return;
        }

        logger.logComment("Combo box state changed: " + e.getItem().toString());

        Cell cell = (Cell) e.getItem();

        if (cell != null)
        {
            logger.logComment("Item selected: " + cell.getInstanceName());
/*
            StringBuffer fullInfo = new StringBuffer();
            fullInfo.append("<b>Cell Type: </b>"+cell.getInstanceName() + "<br>");
            fullInfo.append("<b>Description:</b><br>");

            if (cell.getCellDescription()!=null)
            fullInfo.append(GeneralUtils.replaceAllTokens(cell.getCellDescription(), "\n", "<br>"));

            if (!fullInfo.toString().endsWith("<br>")) fullInfo.append("<br>");
            fullInfo.append("<br>");

            fullInfo.append("<b>Initial potential: </b>"+cell.getInitialPotential().toShortString() +
                            " "+ UnitConverter.voltageUnits[UnitConverter.NEUROCONSTRUCT_UNITS].getSymbol()
                            + "<br>");

            fullInfo.append("<b>Specific Axial Resistance: </b>"+cell.getSpecAxRes().toShortString() +
                " "+ UnitConverter.specificAxialResistanceUnits[UnitConverter.NEUROCONSTRUCT_UNITS].getSymbol()
                + "<br>");

            fullInfo.append("<b>Specific Capacitance: </b>"+cell.getSpecCapacitance().toShortString() +
                            " " + UnitConverter.specificCapacitanceUnits[UnitConverter.NEUROCONSTRUCT_UNITS].getSymbol()
                            + "<br><br>");


            fullInfo.append("<b>Sections:</b>" + cell.getAllSections().size()+ "<br>");

            fullInfo.append("<b>Segments: total: </b>" + cell.getAllSegments().size()
                            + ", <b>somatic: </b> "+cell.getOnlySomaSegments().size()
                            + ", <b>dendritic: </b> "+cell.getOnlyDendriticSegments().size()
                            + ", <b>axonal: </b> "+cell.getOnlyAxonalSegments().size() + "<br>");


            if (cell.getFirstSomaSegment()!=null)
            {
                fullInfo.append("<br><b>Soma's primary section diameter: </b>"
                                + (cell.getFirstSomaSegment().getRadius() * 2) + "<br><br>");
            }
            else
            {
                fullInfo.append("<br><b>Problem getting Soma's primary section diameter: </b><br><br>");
            }


            ArrayList<ChannelMechanism> allChanMechs = cell.getAllChannelMechanisms();
            for (int i = 0; i < allChanMechs.size(); i++)
            {
                ChannelMechanism chanMech =  null;


                chanMech = allChanMechs.get(i);
                Vector groups = cell.getGroupsWithChanMech(chanMech);
                fullInfo.append("-  Channel Mechanism: " + chanMech + " is present on: " + groups + "<br>\n");
            }

            ArrayList<String> allSynapses = cell.getAllAllowedSynapseTypes();
            for (int i = 0; i < allSynapses.size(); i++)
            {
                String syn = allSynapses.get(i);
                Vector groups = cell.getGroupsWithSynapse(syn);
                fullInfo.append("-  Synapse: " + syn + " is allowed on: " + groups + "<br>\n");
            }



            fullInfo.append("<br><b>Length in X direction: </b>"+ CellTopologyHelper.getXExtentOfCell(cell) + "<br>");

            fullInfo.append("<b>Length in Y direction: </b>"+CellTopologyHelper.getYExtentOfCell(cell) + "<br>");

            fullInfo.append("<b>Length in Z direction: </b>"+ CellTopologyHelper.getZExtentOfCell(cell) + "<br>");

            float totalSurfaceArea = 0;
            Vector<Segment> segs = cell.getAllSegments();
            for (int i = 0; i < segs.size(); i++)
            {
                totalSurfaceArea = totalSurfaceArea + segs.elementAt(i).getSegmentSurfaceArea();
            }

            fullInfo.append("<b>Total surface area of all segments: </b>"+ totalSurfaceArea + " "
                      + UnitConverter.areaUnits[UnitConverter.NEUROCONSTRUCT_UNITS].getSymbol()+ "<br>\n");

            fullInfo.append("<br><b>Status of cell: </b>");

            ValidityStatus status = CellTopologyHelper.getValidityStatus(cell);


                fullInfo.append("<br><font color=\""+status.getColour()+"\">" +
                                GeneralUtils.replaceAllTokens(status.getMessage(),
                                                              "\n",
                                                              "<br>")
                                + "</font><br>");


            ValidityStatus bioStatus = CellTopologyHelper.getBiophysicalValidityStatus(cell, this.projManager.getCurrentProject());


                fullInfo.append("<br><font color=\""+bioStatus.getColour()+"\">" +
                                GeneralUtils.replaceAllTokens(bioStatus.getMessage(),
                                                              "\n",
                                                              "</font><br>")
                                + "<br>");

*/

            jEditorPaneCellTypeInfo.setText(CellTopologyHelper.printDetails(cell, this.projManager.getCurrentProject(), true, false));


            jEditorPaneCellTypeInfo.setCaretPosition(0);

        }

        /*
                 //this.jTreeCellDetails.removeAll();

                 CellPrototype cell = (CellPrototype)e.getItem();

                 this.projManager.getCurrentProject().cellManager.setSelectedCellType(cell.getInstanceName());
                 this.jPanelCellTypes.updateUI();
         */

    }

    void jButtonNeuronCreateLocal_actionPerformed(ActionEvent e)
    {
        logger.logComment("Create hoc file button pressed...");

        doCreateHoc(NeuronFileManager.RUN_LOCALLY);

    }

    void jButtonNeuronRun_actionPerformed(ActionEvent e)
    {
        logger.logComment("Run hoc file button pressed...");

        doRunHoc();

    }

    void jButton3DView_actionPerformed(ActionEvent e)
    {
        GeneralUtils.timeCheck("Pressed view 3d");
        doDestroy3D();
        this.doCreate3D(jComboBoxView3DChoice.getSelectedItem());
        GeneralUtils.timeCheck("Done view 3d");
    }

    void jButton3DDestroy_actionPerformed(ActionEvent e)
    {
        doDestroy3D();
    }

    void jButtonAddRegion_actionPerformed(ActionEvent e)
    {
        logger.logComment("Adding new Region...");

        doNewRegion();

    }

    void jButtonCellGroupNew_actionPerformed(ActionEvent e)
    {
        logger.logComment("Creating new Cell Group...");

        doNewCellGroup();
    }

 //   void jButtonConstructCell3DDemo_actionPerformed(ActionEvent e)
 //   {
 //       logger.logComment("Constructing model of one cell");
  //      doCreate3DCell();
 //   }

    /*
        void jTextAreaProjDescription_keyPressed(KeyEvent e)
        {
            logger.logComment("Key Pressed in project Description...");
            projManager.getCurrentProject().setProjectDescription(this.jTextAreaProjDescription.getText());
            this.refreshGeneral();

        }
     */
    void jMenuItemCloseProject_actionPerformed(ActionEvent e)
    {
        logger.logComment("Closing current project");
        boolean continueClosing = checkToSave();
        if (!continueClosing) return;
        closeProject();
    }

    void jButtonPreferences_actionPerformed(ActionEvent e)
    {
        logger.logComment("Preferences button pressed...");
        doOptionsPane(OptionsFrame.THREE_D_PREFERENCES, OptionsFrame.PROJECT_PROPERTIES_MODE);
    }


    void jButtonToggleTips_actionPerformed(ActionEvent e)
    {
        logger.logComment("Toggle Tips button pressed...");

        GeneralUtils.printMemory(true);

        ToolTipManager ttm = ToolTipManager.sharedInstance();

        if (ttm.isEnabled())
        {
            ttm.setEnabled(false);
            logger.logComment("Turning off tool tips");
            jButtonToggleTips.setIcon(imageNoTips);
        }
        else
        {
            ttm.setEnabled(true);
            logger.logComment("Turning on tool tips");
            jButtonToggleTips.setIcon(imageTips);
        }

        System.gc();
        System.gc();

    }


    void jButtonToggleConsoleOut_actionPerformed(ActionEvent e)
    {
        logger.logComment("Toggle Console out button pressed...");

        if (GeneralProperties.getLogFilePrintToScreenPolicy())
        {
            GeneralProperties.setLogFilePrintToScreenPolicy(false);
            jButtonToggleConsoleOut.setIcon(imageNoConsoleOut);
        }
        else
        {
            GeneralProperties.setLogFilePrintToScreenPolicy(true);
            jButtonToggleConsoleOut.setIcon(imageConsoleOut);
        }

    }


    /**
     * Needed when tool tips state changes in OptionsFrame...
     */
    protected void alertChangeToolTipsState()
    {
        ToolTipManager ttm = ToolTipManager.sharedInstance();

        if (ttm.isEnabled())
        {
            ttm.setEnabled(true);
            jButtonToggleTips.setIcon(imageTips);
        }
        else
        {
            ttm.setEnabled(false);
            jButtonToggleTips.setIcon(imageNoTips);
        }
    }


    /**
     * Needed when console out state changes in OptionsFrame...
     */
    protected void updateConsoleOutState()
    {
        if (GeneralProperties.getLogFilePrintToScreenPolicy())
        {
            jButtonToggleConsoleOut.setIcon(imageConsoleOut);
        }
        else
        {
            jButtonToggleConsoleOut.setIcon(imageNoConsoleOut);
        }

    }



    void jMenuItemProjProperties_actionPerformed(ActionEvent e)
    {
        logger.logComment("Project Preferences menu item selected...");
        doOptionsPane(OptionsFrame.THREE_D_PREFERENCES, OptionsFrame.PROJECT_PROPERTIES_MODE);
    }
/*
    void jTextFieldDuration_keyReleased(KeyEvent e)
    {
        logger.logComment("Duration of simulation changed...");

        try
        {
            String text = jTextFieldDuration.getText();
            if (text.length() == 0)
            {
                logger.logComment("They've removed the text...");
                jTextFieldDuration.setText("0");
            }
            else
            {
                Float.parseFloat(text);
            }
            createSimulationSummary();
        }
        catch (NumberFormatException ex)
        {
            GuiUtils.showErrorMessage(logger,
                "Please enter a correctly formatted number for the simulation duration.", ex, this);
        }

    }
*/
    void jTextFieldDT_keyReleased(KeyEvent e)
    {
        logger.logComment("Value of DT changed...");

        try
        {
            Float.parseFloat(jTextFieldSimDT.getText());
            createSimulationSummary();
        }
        catch (NumberFormatException ex)
        {
            GuiUtils.showErrorMessage(logger, "Please enter a correctly formatted number for dt.",
                                      ex, this);
        }

    }


    void jButton3DSettings_actionPerformed(ActionEvent e)
    {
        logger.logComment("Setting 3D options...");
        this.doOptionsPane(OptionsFrame.THREE_D_PREFERENCES, OptionsFrame.PROJECT_PROPERTIES_MODE);

    }

    void jButtonNeuronView_actionPerformed(ActionEvent e)
    {
        logger.logComment("Going to display the selected hoc file...");

        if (projManager.getCurrentProject() == null)
        {
            logger.logError("No project loaded...");
            return;
        }

        try
        {
            String selectedFile = (String)jComboBoxNeuronFiles.getSelectedItem();
            File file = new File(ProjectStructure.getNeuronCodeDir(projManager.getCurrentProject().getProjectMainDirectory()), selectedFile);
            SimpleViewer.showFile(file.getAbsolutePath(), 12, false, false, true);
        }
        catch (Exception ex)
        {
            GuiUtils.showErrorMessage(logger, "Hoc file not yet generated", ex, this);
        }


    }

    void jButtonNetSetAddNew_actionPerformed(ActionEvent e)
    {
        logger.logComment("Adding new Net connection...");

        doNewNetConnection();

    }

    void jButtonCellTypeViewCell_actionPerformed(ActionEvent e)
    {

        Cell selectedCell = (Cell) jComboBoxCellTypes.getSelectedItem();
        if (selectedCell.getInstanceName().equals(cellComboPrompt))
        {
            return;
        }

        logger.logComment("Changing to the view for cell: " + selectedCell.getInstanceName());

        jPanel3DMain.removeAll();
        base3DPanel = null;

        this.validate();

        jTabbedPaneMain.setSelectedIndex(jTabbedPaneMain.indexOfTab(this.VISUALISATION_TAB));

        jComboBoxView3DChoice.setSelectedItem(selectedCell);

        //this.jButtonConstructCell3DDemo_actionPerformed(null);
        doCreate3D(selectedCell);
    }

    void jButtonCellTypeViewCellChans_actionPerformed(ActionEvent e)
    {

        Cell selectedCell = (Cell) jComboBoxCellTypes.getSelectedItem();
        if (selectedCell.getInstanceName().equals(cellComboPrompt))
        {
            return;
        }

        logger.logComment("Changing to the view for cell: " + selectedCell.getInstanceName());

        jPanel3DMain.removeAll();
        base3DPanel = null;

        this.validate();

        jTabbedPaneMain.setSelectedIndex(jTabbedPaneMain.indexOfTab(this.VISUALISATION_TAB));

        jComboBoxView3DChoice.setSelectedItem(selectedCell);

        //this.jButtonConstructCell3DDemo_actionPerformed(null);
        doCreate3D(selectedCell);

        if (base3DPanel instanceof OneCell3DPanel)
        {
            ((OneCell3DPanel)base3DPanel).setHighlighted(OneCell3DPanel.highlightDensMechs);
        }

    }





    void jButtonGenerate_actionPerformed(ActionEvent e)
    {
        doGenerate();
    }


    void jButtonGenerateLoad_actionPerformed(ActionEvent e)
    {
        logger.logComment("Loading a previously generated network...");

        final JFileChooser chooser = new JFileChooser();


        chooser.setCurrentDirectory(ProjectStructure.getSavedNetworksDir(projManager.getCurrentProject().getProjectMainDirectory()));

        chooser.setFileFilter(new SimpleFileFilter(new String[]{ProjectStructure.getNeuroMLFileExtension(),
                                                   ProjectStructure.getNeuroMLCompressedFileExtension()}, "(Compressed) NetworkML files", true));

        logger.logComment("chooser.getCurrentDirectory(): "+chooser.getCurrentDirectory());

        chooser.setDialogTitle("Choose (zipped) NetworkML file to load");

        final JTextArea summary = new JTextArea(12,40);
        summary.setMargin(new Insets(5,5,5,5));
        summary.setEditable(false);
        JScrollPane jScrollPane = new JScrollPane(summary);
        //jScrollPane.setBorder(BorderFactory.createEtchedBorder());

        chooser.addPropertyChangeListener(new PropertyChangeListener(){

            public void propertyChange(PropertyChangeEvent e)
            {
                logger.logComment("propertyChange: " + e);
                logger.logComment("getPropertyName: " + e.getPropertyName());
                if (e.getPropertyName().equals("SelectedFileChangedProperty"))
                {
                    File newFile = chooser.getSelectedFile();
                    logger.logComment("Looking at: " + newFile);
                    try
                    {
                        if (newFile.getName().endsWith(ProjectStructure.getNeuroMLCompressedFileExtension()))
                        {
                            ZipInputStream zf = new ZipInputStream(new FileInputStream( newFile));
                            ZipEntry ze = null;

                            //summary.setText("Comment: "+zf.getNextEntry().getComment());
                            while ((ze=zf.getNextEntry())!=null)
                            {
                                logger.logComment("Entry: " +ze );
                                summary.setText("Contains: "+ze);
                            }
                            summary.setCaretPosition(0);


                        }
                        else
                        {

                            FileReader fr = null;

                            fr = new FileReader(newFile);

                            LineNumberReader reader = new LineNumberReader(fr);
                            String nextLine = null;

                            StringBuilder sb = new StringBuilder();
                            int count = 0;
                            int maxlines = 100;

                            while (count <= maxlines && (nextLine = reader.readLine()) != null)
                            {
                                sb.append(nextLine + "\n");
                                count++;
                            }
                            if (count >= maxlines) sb.append("\n\n  ... NetworkML file continues ...");
                            reader.close();
                            fr.close();
                            summary.setText(sb.toString());
                            summary.setCaretPosition(0);

                        }
                    }
                    catch (Exception ex)
                    {
                        summary.setText("Error loading contents of file: " + newFile);
                    }
                }

            }

            });

        chooser.setAccessory(jScrollPane);

        int retval = chooser.showDialog(this, "Choose network");

        if (retval == JOptionPane.OK_OPTION)
        {
            try
            {
                projManager.getCurrentProject().resetGenerated();

                logger.logComment("Removing 3D network, as it's no longer relevant...");
                doDestroy3D();

                if (this.jCheckBoxRandomGen.isSelected())
                {
                    Random tempRandom = new Random();
                    this.jTextFieldRandomGen.setText(tempRandom.nextInt() + "");
                }

                /** @todo Put most of this is in ProjectManager... */


                InputSource is = null;

                SAXParserFactory spf = SAXParserFactory.newInstance();
                spf.setNamespaceAware(true);

                XMLReader xmlReader = null;

                xmlReader = spf.newSAXParser().getXMLReader();

                NetworkMLReader nmlBuilder
                    = new NetworkMLReader(projManager.getCurrentProject().generatedCellPositions,
                                          projManager.getCurrentProject().generatedNetworkConnections);

                xmlReader.setContentHandler(nmlBuilder);

                if (chooser.getSelectedFile().getName().endsWith(ProjectStructure.getNeuroMLCompressedFileExtension()))
                {
                    FileInputStream instream = new FileInputStream(chooser.getSelectedFile());

                    ZipInputStream zis = new ZipInputStream(new BufferedInputStream(instream));

                    ZipEntry entry = zis.getNextEntry();

                    logger.logComment("Reading contents of zip: " + entry);

                    is = new InputSource(zis);
                }
                else
                {
                   FileInputStream instream = new FileInputStream(chooser.getSelectedFile());
                   is = new InputSource(instream);
                }

                xmlReader.parse(is);

                String prevSimConfig = nmlBuilder.getSimConfig();
                long randomSeed = nmlBuilder.getRandomSeed();

                if (randomSeed!=Long.MIN_VALUE)
                {
                    this.jTextFieldRandomGen.setText(randomSeed+"");
                    ProjectManager.setRandomGeneratorSeed(randomSeed);
                    ProjectManager.reinitialiseRandomGenerator();
                }
                if (prevSimConfig!=null)
                {
                    this.jComboBoxSimConfig.setSelectedItem(prevSimConfig);
                }

            }
            catch (Exception ex)
            {
                GuiUtils.showErrorMessage(logger, "Error loading network info from: "+chooser.getSelectedFile(), ex, this);
                return;
            }

            SimConfig simConfig = getSelectedSimConfig();

            jEditorPaneGenerateInfo.setText("Cell positions and network connections loaded from: <b>"+chooser.getSelectedFile()+"</b><br><br>"
                                            +"<center><b>Cell Groups:</b></center>"
                                            +projManager.getCurrentProject().generatedCellPositions.getHtmlReport()
                                            +"<center><b>Network Connections:</b></center>"
                +projManager.getCurrentProject().generatedNetworkConnections.getHtmlReport(
                                        GeneratedNetworkConnections.ANY_NETWORK_CONNECTION,simConfig));



            projManager.elecInputGenerator = new ElecInputGenerator(projManager.getCurrentProject(), this);

            projManager.elecInputGenerator.setSimConfig(simConfig);

            projManager.elecInputGenerator.start();

            sourceOfCellPosnsInMemory = NETWORKML_POSITIONS;

            jComboBoxView3DChoice.setSelectedItem(LATEST_GENERATED_POSITIONS);

        }


    }

    void jButtonGenerateSave_actionPerformed(ActionEvent e)
    {
        logger.logComment("Saving the currently generated network");

        logger.logComment("saving the network in NeuroML form...");

        String origText = jButtonGenerateSave.getText();
        jButtonGenerateSave.setText("Saving...");
        jButtonGenerateSave.setEnabled (false);

        File savedNetsDir = ProjectStructure.getSavedNetworksDir(projManager.getCurrentProject().getProjectMainDirectory());

        String timeInfo = GeneralUtils.getCurrentDateAsNiceString() +"_"+GeneralUtils.getCurrentTimeAsNiceString();
        timeInfo = GeneralUtils.replaceAllTokens(timeInfo, ":", "-");

        String fileName = "Net_" +timeInfo;

        fileName = JOptionPane.showInputDialog("Please enter the name of the NetworkML file (without "
                                               +ProjectStructure.getNeuroMLFileExtension()+" extension)",fileName);

        if (fileName == null) return;


        File networkFile = new File(savedNetsDir, fileName+ ProjectStructure.getNeuroMLFileExtension());

        if (networkFile.exists())
        {

            int goOn = JOptionPane.showConfirmDialog(this, "File: "+ networkFile+" already exists! Overwrite?",
                                                     "Overwrite file?", JOptionPane.YES_NO_OPTION);

            if (goOn == JOptionPane.NO_OPTION)
            {

                jButtonGenerateSave.setText(origText);
                jButtonGenerateSave.setEnabled(true);
                return;

            }
        }

        StringBuffer notes = new StringBuffer("\nNetwork structure for project: "
                                                         +projManager.getCurrentProject().getProjectName() + " saved with neuroConstruct v"+
                                                         GeneralProperties.getVersionNumber()+" on: "+ GeneralUtils.getCurrentTimeAsNiceString() +", "
                                    + GeneralUtils.getCurrentDateAsNiceString()+"\n\n");


        Iterator<String> cellGroups = projManager.getCurrentProject().generatedCellPositions.getNamesGeneratedCellGroups();

        while (cellGroups.hasNext())
        {
            String cg = cellGroups.next();
            int numHere = projManager.getCurrentProject().generatedCellPositions.getNumberInCellGroup(cg);
            if (numHere>0)
                notes.append("Cell Group: "+cg+" contains "+numHere+" cells\n");

        }
        notes.append("\n");

        Iterator<String> netConns = projManager.getCurrentProject().generatedNetworkConnections.getNamesNetConns();

        while (netConns.hasNext())
        {
            String mc = netConns.next();
            int numHere = projManager.getCurrentProject().generatedNetworkConnections.getSynapticConnections(mc).size();
            if (numHere>0)
                notes.append("Network connection: "+mc+" contains "+numHere+" individual synaptic connections\n");

        }
        notes.append("\n");


        try
        {

            projManager.getCurrentProject().saveNetworkStructure(networkFile,
                                                                 notes.toString(),
                                                                 this.jCheckBoxGenerateZip.isSelected(),
                                                                 this.jCheckBoxGenerateExtraNetComments.isSelected(),
                                                                 getSelectedSimConfig().getName());
        }
        catch (NeuroMLException ex1)
        {
            GuiUtils.showErrorMessage(logger, "Problem saving network in NeuroML", ex1, this);
        }



        jButtonGenerateSave.setText(origText);
        jButtonGenerateSave.setEnabled(true);


        refreshTabGenerate();

        return;

    }

    void jButtonSimConfigEdit_actionPerformed(ActionEvent e)
    {
        SimConfigManager dlg
            = new SimConfigManager(projManager.getCurrentProject().simConfigInfo,
                                   this,
                                   projManager.getCurrentProject());

        dlg.pack();

        Dimension screenSize = Toolkit.getDefaultToolkit().getScreenSize();
        Dimension frameSize = dlg.getSize();

        if (frameSize.height > screenSize.height)
            frameSize.height = screenSize.height;
        if (frameSize.width > screenSize.width)
            frameSize.width = screenSize.width;

        dlg.setLocation((screenSize.width - frameSize.width) / 2, (screenSize.height - frameSize.height) / 2);


        dlg.setVisible(true);

        dlg.setSelectedSimConfig(getSelectedSimConfig().getName());

        this.refreshAll();

    }

    private SimConfig getSelectedSimConfig()
    {
        String selectedSimConfig = (String)this.jComboBoxSimConfig.getSelectedItem();

        return  projManager.getCurrentProject().simConfigInfo.getSimConfig(selectedSimConfig);

    }


    private void doGenerate()
    {
        logger.logComment("Going to generate network...");
        if (!projManager.projectLoaded()) return;


        projManager.getCurrentProject().simConfigInfo.validateStoredSimConfigs(projManager.getCurrentProject());

        SimConfig simConfig = getSelectedSimConfig();

        if (simConfig.getCellGroups().size() == 0)
        {
            GuiUtils.showErrorMessage(logger,
                                      "Please add one or more Cell groups to Sim Config: "
                                      +simConfig.getName()+" before proceeding", null, this);

        }

        jProgressBarGenerate.setEnabled(true);

        logger.logComment("simConfig: "+simConfig.getName());
        logger.logComment("getCellGroups: "+simConfig.getCellGroups());
        logger.logComment("getNetConns: "+simConfig.getNetConns());


        int totalSteps = simConfig.getCellGroups().size()
            + simConfig.getNetConns().size()
            + simConfig.getInputs().size();

        jProgressBarGenerate.setMaximum(totalSteps * 100);
        jProgressBarGenerate.setValue(0);
        jProgressBarGenerate.setString("progress...");

        this.jButtonGenerateStop.setEnabled(true);

        logger.logComment("Removing 3D network, as it's no longer relevant...");
        doDestroy3D();

        if (this.jCheckBoxRandomGen.isSelected())
        {
            Random tempRandom = new Random();
            this.jTextFieldRandomGen.setText(tempRandom.nextInt()+"");
        }

        long seed = 0;
        try
        {
            seed = Long.parseLong(jTextFieldRandomGen.getText());
        }
        catch (NumberFormatException ex)
        {
            GuiUtils.showErrorMessage(logger, "Please enter a valid long integer into the"
                                      +" field for the random number generator seed", ex, this);
            return;
        }

        projManager.doGenerate(simConfig.getName(), seed);

        this.jButton3DView.setEnabled(true);
        this.jButton3DPrevSims.setEnabled(true);
        jEditorPaneGenerateInfo.setText("Generating cell positions and network connections. Please wait...");

        sourceOfCellPosnsInMemory = GENERATED_POSITIONS;

        jComboBoxView3DChoice.setSelectedItem(LATEST_GENERATED_POSITIONS);

        // need this to update list of 3d position files...
        refreshTab3D();

    }

    void jButtonRegionRemove_actionPerformed(ActionEvent e)
    {
        logger.logComment("Going to remove region...");

        doRemoveRegion();

        refreshTabRegionsInfo();

    }

    void jButtonCellGroupDelete_actionPerformed(ActionEvent e)
    {
        logger.logComment("Going to remove cell group...");
        int selectedRow = jTableCellGroups.getSelectedRow();

        if (selectedRow < 0)
        {
            logger.logComment("No row selected...");
            return;
        }

        String cellGroupName = (String) projManager.getCurrentProject().cellGroupsInfo.getValueAt(selectedRow,
            CellGroupsInfo.COL_NUM_CELLGROUPNAME);

        doRemoveCellGroup(cellGroupName);
        refreshTabCellGroupInfo();

    }

    void jButtonNetConnDelete_actionPerformed(ActionEvent e)
    {
        logger.logComment("Going to remove net conn...");
        doRemoveNetConn();
        refreshTabNetSettings();

    }

    void jMenuItemNmodlEditor_actionPerformed(ActionEvent e)
    {
        logger.logComment("nmodlEditor to run...");

        String dir = null;

        if (projManager.getCurrentProject()!=null)
            dir = projManager.getCurrentProject().getProjectMainDirectory().getAbsolutePath();
        else
            dir = ProjectStructure.getnCProjectsDirectory().getAbsolutePath();

        new NmodlEditorApp(dir);
    }
/*
    void jButtonSynapseAdd_actionPerformed(ActionEvent e)
    {
        logger.logComment("Adding new Synapse Type");
        doCreateNewSynapseType();
        refreshTabNmodl();
    }*/
/*
    void jRadioButtonStimIClamp_itemStateChanged(ItemEvent e)
    {
        logger.logComment("jRadioButtonStimIClamp altered: ");
        if (e.getStateChange() == ItemEvent.SELECTED)
        {

            logger.logComment("Adding stuff for IClamp...");
            jPanelSimParams.removeAll();
            JPanel tempPanel1 = new JPanel();
            tempPanel1.setSize(500, 30);
            tempPanel1.add(new JLabel("Amplitude of stimulation: "));
            tempPanel1.add(jTextFieldIClampAmplitude);
            jPanelSimParams.add(tempPanel1);
            JPanel tempPanel2 = new JPanel();
            tempPanel2.setSize(500, 30);
            tempPanel2.add(new JLabel("Duration of stimulation: "));
            tempPanel2.add(jTextFieldIClampDuration);
            jPanelSimParams.add(tempPanel2);
            jPanelSimParams.validate();
        }
    }

    void jRadioButtonStimNone_itemStateChanged(ItemEvent e)
    {
        logger.logComment("jRadioButtonStimNone altered: ");
        if (e.getStateChange() == ItemEvent.SELECTED)
        {
            jPanelSimParams.removeAll();
            JPanel tempPanel1 = new JPanel();
            tempPanel1.setSize(600, 100);
            jPanelSimParams.add(tempPanel1);

            jPanelSimParams.validate();
        }

    }

    void jRadioButtonStimNetStim_itemStateChanged(ItemEvent e)
    {
        logger.logComment("jRadioButtonStimNetStim altered: ");
        if (e.getStateChange() == ItemEvent.SELECTED)
        {
            logger.logComment("Adding stuff for NetStim...");
            jPanelSimParams.removeAll();
            JPanel tempPanel1 = new JPanel();
            tempPanel1.setSize(500, 30);
            tempPanel1.add(new JLabel("Average number of spikes"));
            tempPanel1.add(jTextFieldNetStimNumber);
            jPanelSimParams.add(tempPanel1);
            JPanel tempPanel2 = new JPanel();
            tempPanel2.setSize(500, 30);
            tempPanel2.add(new JLabel("Noise (0: no noise -> 1: noisiest)"));
            tempPanel2.add(jTextFieldNetStimNoise);
            jPanelSimParams.add(tempPanel2);
            jPanelSimParams.validate();
        }

    }
*/
/*
    void jButtonSynapseEdit_actionPerformed(ActionEvent e)
    {
        int selectedRow = jTableSynapses.getSelectedRow();

        if (selectedRow < 0)
        {
            logger.logComment("No row selected...");
            return;
        }
        String type = (String)projManager.getCurrentProject().synapticProcessInfo.getValueAt(selectedRow, SynapticProcessInfo.COL_NUM_TYPE);


        String filename = (String)projManager.getCurrentProject().synapticProcessInfo.getValueAt(selectedRow, SynapticProcessInfo.COL_NUM_FILE);

        logger.logComment("Filename corresponding to row "+selectedRow+": "+ filename);

        NmodlEditorApp nmodlApp = new NmodlEditorApp(projManager.getCurrentProject().getProjectMainDirectory().getAbsolutePath());
        if (type.equals("Inbuilt synapse"))
        {
            GuiUtils.showInfoMessage(logger, "Information", "Note that this is an inbuilt NEURON synapse, and cannot be changed./nTo create a synapse <i>like</i> this select Add Custom Synapse and select this file as a template", this);
            nmodlApp.editModFile(filename, true);
        }
        else nmodlApp.editModFile(filename, false);
    }
*/
    void jMenuItemGeneralProps_actionPerformed(ActionEvent e)
    {
        logger.logComment("General Preferences menu item selected...");
        doOptionsPane(OptionsFrame.GENERAL_PREFERENCES, OptionsFrame.GENERAL_PROPERTIES_MODE);

    }


    public void jMenuRecentFile_actionPerformed(ActionEvent e)
    {
        logger.logComment("Action event: "+e);
        JMenuItem menuItem =(JMenuItem)e.getSource();
        String recentFileName = menuItem.getText();
        logger.logComment("Opening recent file: "+recentFileName);

        File recentFile = new File(recentFileName);

        if (!recentFile.exists())
        {
            GuiUtils.showErrorMessage(logger, "The file: "+recentFileName+" doesn't exist...", null, this);
            recentFiles.removeFromList(recentFile);
            refreshAll();
            return;
        }

        boolean continueClosing = checkToSave();
        if (!continueClosing) return;
        closeProject();

        initialisingProject = true;

        try
        {
            projManager.setCurrentProject(Project.loadProject(recentFile, this));
            logger.logComment("---------------  Proj status: "+ projManager.getCurrentProject().getProjectStatusAsString());
        }
        catch (ProjectFileParsingException ex2)
        {
            recentFiles.removeFromList(recentFile);
            GuiUtils.showErrorMessage(logger, ex2.getMessage(), ex2, this);
            initialisingProject = false;
            closeProject();
            return;
        }

        // to make sure it's first...
        recentFiles.addToList(recentFile.getAbsolutePath());

        refreshAll();
        enableTableCellEditingFunctionality();


        initialisingProject = false;
        createSimulationSummary();

        jTabbedPaneMain.setSelectedIndex(0); // main tab...

    }

    void jButtonGenerateStop_actionPerformed(ActionEvent e)
    {
        logger.logComment("Telling the cell posn generator to stop...");
        projManager.cellPosnGenerator.stopGeneration();
        if (projManager.netConnGenerator!=null)
        {
            logger.logComment("Telling the net conn generator to stop...");
            this.projManager.netConnGenerator.stopGeneration();
        }
        if (projManager.arbourConnectionGenerator!=null)
        {
            logger.logComment("Telling the arbourConnectionGenerator to stop...");
            this.projManager.arbourConnectionGenerator.stopGeneration();
        }

        if (projManager.elecInputGenerator!=null)
        {
            logger.logComment("Telling the elecInputGenerator to stop...");
            this.projManager.elecInputGenerator.stopGeneration();
        }



        jProgressBarGenerate.setValue(0);
        jProgressBarGenerate.setString("Generation stopped");

    }

    void jButtonNetConnEdit_actionPerformed(ActionEvent e)
    {
        logger.logComment("Going to edit net conn...");
        doEditNetConn();
        refreshTabNetSettings();
    }

    void jButtonAnalyseConns_actionPerformed(ActionEvent e)
    {
        logger.logComment("Analyse lengths pressed...");
        String selectedItem = (String) jComboBoxAnalyseNetConn.getSelectedItem();
        if (selectedItem.equals(defaultAnalyseNetConnString))
        {
            GuiUtils.showErrorMessage(logger,
                                      "Please select which Network Connection whose lengths you would like to analyse", null, this);
            return;
        }

        projManager.doAnalyseLengths(selectedItem);
    }

    void jButtonAnalyseNumConns_actionPerformed(ActionEvent e)
    {
        logger.logComment("Analyse numbers of connections pressed...");

        doAnalyseNumConns();


    }

    void jButtonAnalyseCellDensities_actionPerformed(ActionEvent e)
    {
        logger.logComment("Analyse cell densities pressed...");

        doAnalyseCellDensities();

    }


/*
    void jButtonChanMechEdit_actionPerformed(ActionEvent e)
    {
        int selectedRow = jTableChanMechs.getSelectedRow();

        if (selectedRow < 0)
        {
            logger.logComment("No row selected...");
            return;
        }
        String filename = (String)projManager.getCurrentProject().channelMechanismInfo.getValueAt(selectedRow, ChannelMechanismInfo.COL_NUM_FILE);
        String type = (String)projManager.getCurrentProject().channelMechanismInfo.getValueAt(selectedRow, ChannelMechanismInfo.COL_NUM_TYPE);

        logger.logComment("Filename corresponding to row "+selectedRow+": "+ filename);


        NmodlEditorApp nmodlApp = new NmodlEditorApp(projManager.getCurrentProject().getProjectMainDirectory().getAbsolutePath());
        if (type.equals("Inbuilt channel mechanism"))
        {
            GuiUtils.showInfoMessage(logger, "Information", "Note that this is an inbuilt NEURON channel mechanism, and cannot be changed.\nTo create a channel mechanism <i>like</i> this select Add Channel Mechanism and select this file as a template", this);
            nmodlApp.editModFile(filename, true);
        }
        else nmodlApp.editModFile(filename, false);


    }

    void jButtonChanMechAdd_actionPerformed(ActionEvent e)
    {
        logger.logComment("Adding new Chan Mech");
        doCreateNewChannelMechanism();
        ///refreshTabNmodl();

    }*/

    void jMenuItemZipUp_actionPerformed(ActionEvent e)
    {
        boolean allOk = checkToSave();

        if (!allOk) return; // i.e. cancelled...

        logger.logComment("Zipping up the project...");
        String nameOfZippedFile = null;
        //String projectDir = null;

        nameOfZippedFile = projManager.getCurrentProject().getProjectMainDirectory()
            + System.getProperty("file.separator")
            + projManager.getCurrentProject().getProjectName()
            + ProjectStructure.getProjectZipFileExtension();

        ProjectManager.zipDirectoryContents(projManager.getCurrentProject().getProjectMainDirectory(),
            nameOfZippedFile);
    }


    void jMenuItemUnzipProject_actionPerformed(ActionEvent e)
    {
        doUnzipProject();
    }


    void jComboBoxView3DChoice_popupMenuWillBecomeVisible(PopupMenuEvent e)
    {

        logger.logComment("popupMenuWillBecomeVisible pressed...");
        refreshView3DComboBox();

    }

    void jButtonCellGroupsEdit_actionPerformed(ActionEvent e)
    {
        logger.logComment("Editing a cell group...");

        doEditCellGroup();
    }

    void jButtonNetAAAdd_actionPerformed(ActionEvent e)
    {
        logger.logComment("Adding new AA net con...");

        doNewAAConn();
    }

    void jButtonNetAAEdit_actionPerformed(ActionEvent e)
    {
        logger.logComment("Going to edit net conn...");
        doEditVolConn();
        refreshTabNetSettings();

    }

    void jButtonNetAADelete_actionPerformed(ActionEvent e)
    {
        logger.logComment("Going to remove net conn...");
        doRemoveAANetConn();
        refreshTabNetSettings();

    }

    void jMenuItemViewProjSource_actionPerformed(ActionEvent e)
    {
        logger.logComment("Going to show project file...");
        boolean proceed = checkToSave();
        if (proceed)
        {
            try
            {
                SimpleXMLDocument sourceXML = SimpleXMLReader.getSimpleXMLDoc(projManager.getCurrentProject().getProjectFile());

                SimpleViewer.showString(sourceXML.getXMLString("    ", true),
                                        "neuroConstruct project file:" + projManager.getCurrentProject().getProjectFullFileName(),
                                      12,
                                      false,
                                      true);
            }
            catch (Exception ex)
            {
                GuiUtils.showErrorMessage(logger, "Problem showing project source file...", ex, this);
            }
        }

    }

    void jMenuItemGlossary_actionPerformed(ActionEvent e)
    {
        logger.logComment("Going to show glossary...");

        File f = new File(ProjectStructure.getGlossaryHtmlFile());
        try
        {
            HelpFrame simpleViewer = HelpFrame.showFrame(f.toURL(), f.getAbsolutePath(), false);


            simpleViewer.setFrameSize(800, 600);

            Dimension screenSize = Toolkit.getDefaultToolkit().getScreenSize();
            Dimension frameSize = simpleViewer.getSize();

            if (frameSize.height > screenSize.height)
                frameSize.height = screenSize.height;
            if (frameSize.width > screenSize.width)
                frameSize.width = screenSize.width;

            simpleViewer.setLocation( (screenSize.width - frameSize.width) / 2,
                                     (screenSize.height - frameSize.height) / 2);

            simpleViewer.setVisible(true);

        }
        catch(IOException io)
        {
            GuiUtils.showErrorMessage(logger, "Problem showing help frame", io, this);
        }




    }


    void jButtonCellTypeViewInfo_actionPerformed(ActionEvent e)
    {
        Cell cell = (Cell)jComboBoxCellTypes.getSelectedItem();

        boolean html = true;


        if (cell.getAllSegments().size()>500) html = false;

        SimpleViewer.showString(CellTopologyHelper.printDetails(cell, this.projManager.getCurrentProject(), html),
                                "Cell Info for: "+ cell.toString(), 11, false, html);


    }


    void jButtonCellTypeCompare_actionPerformed(ActionEvent e)
    {

        Cell cellTypeToComp = (Cell) jComboBoxCellTypes.getSelectedItem();



        ArrayList<String> names = projManager.getCurrentProject().cellManager.getAllCellTypeNames();

        if (names.size()==1)
        {
            GuiUtils.showErrorMessage(logger, "There is only a single cell in this project, nothing to compare it to.", null, this);
            return;
        }

        String[] otherNames = new String[names.size()-1];
        int count = 0;
        for (int i = 0; i < names.size(); i++)
        {
            if (!names.get(i).equals(cellTypeToComp.getInstanceName()))
            {
                otherNames[count] = names.get(i);
                count++;
            }
        }

        String selection = (String) JOptionPane.showInputDialog(this,
                                                                "Please select the Cell Type to compare " +
                                                                cellTypeToComp.getInstanceName() + " to",
                                                                "Select Cell Type",
                                                                JOptionPane.QUESTION_MESSAGE,
                                                                null,
                                                                otherNames,
                                                                otherNames[0]);

        Cell otherCell = projManager.getCurrentProject().cellManager.getCell(selection);

        String comp = CellTopologyHelper.compare(cellTypeToComp, otherCell);

        SimpleViewer.showString(comp, "Comparison of "+cellTypeToComp+" with "+ otherCell, 12, false, false);

    }


    void jButtonCellTypeDelete_actionPerformed(ActionEvent e)
    {

        Cell cellTypeToDelete = (Cell) jComboBoxCellTypes.getSelectedItem();

        logger.logComment("Deleting cell: "+ cellTypeToDelete);

        if (cellTypeToDelete==null)
        {
            GuiUtils.showErrorMessage(logger, "Problem deleting that Cell Type", null, this);
            return;
        }

        Object[] options1 = {"Continue", "Cancel"};

        JOptionPane option1 = new JOptionPane("Warning, if you delete this Cell Type, recorded simulations which use this Cell Type will not work anymore!",
                                             JOptionPane.DEFAULT_OPTION,
                                             JOptionPane.WARNING_MESSAGE,
                                             null,
                                             options1,
                                             options1[0]);

        JDialog dialog1 = option1.createDialog(this, "Warning");
        dialog1.setVisible(true);

        Object choice = option1.getValue();
        logger.logComment("User has chosen: " + choice);
        if (choice.equals("Cancel")) return;

        Vector cellGroupsUsingIt = projManager.getCurrentProject().cellGroupsInfo.getCellGroupsUsingCellType(cellTypeToDelete.getInstanceName());

        if (cellGroupsUsingIt.size() > 0)
        {
            StringBuffer errorString = new StringBuffer("The Cell Group");
            if (cellGroupsUsingIt.size() > 1) errorString.append("s: ");
            else errorString.append(": ");
            String buttonText = null;

            for (int i = 0; i < cellGroupsUsingIt.size(); i++)
            {
                errorString.append(" " + cellGroupsUsingIt.elementAt(i));
                if (i < cellGroupsUsingIt.size() - 1) errorString.append(", ");
            }
            if (cellGroupsUsingIt.size() > 1)
            {
                errorString.append(" use Cell Type: " + cellTypeToDelete.getInstanceName() + ". Delete these too?");
                buttonText = "Delete Cell Groups";
            }
            else
            {
                errorString.append(" uses Cell Type: " + cellTypeToDelete.getInstanceName() + ". Delete this too?");
                buttonText = "Delete Cell Group";
            }

            Object[] options2 =
                {buttonText, "Cancel All"};

            JOptionPane option2 = new JOptionPane(errorString.toString(),
                                                 JOptionPane.DEFAULT_OPTION,
                                                 JOptionPane.WARNING_MESSAGE,
                                                 null,
                                                 options2,
                                                 options2[0]);

            JDialog dialog2 = option2.createDialog(this, "Warning");
            dialog2.setVisible(true);

            Object choice2 = option2.getValue();
            logger.logComment("User has chosen: " + choice2);
            if (choice2.equals("Cancel All"))
            {
                logger.logComment("User has changed their mind...");
                return;
            }

            for (int i = 0; i < cellGroupsUsingIt.size(); i++)
            {
                String nextCellGroup = (String) cellGroupsUsingIt.elementAt(i);
                logger.logComment("Deleting: " + nextCellGroup);

                doRemoveCellGroup(nextCellGroup);

            }
        }

        projManager.getCurrentProject().cellManager.deleteCellType(cellTypeToDelete);

        File cellFileXml = new File(ProjectStructure.getMorphologiesDir(projManager.getCurrentProject().
                                                                        getProjectMainDirectory())
                                    , cellTypeToDelete.getInstanceName() + ProjectStructure.getJavaXMLFileExtension());

        cellFileXml.delete();

        File cellFileObj = new File(ProjectStructure.getMorphologiesDir(projManager.getCurrentProject().
                                                                        getProjectMainDirectory())
                                    , cellTypeToDelete.getInstanceName() + ProjectStructure.getJavaObjFileExtension());

        cellFileObj.delete();



        projManager.getCurrentProject().markProjectAsEdited();

        refreshTabCellTypes();
    }

    void jButtonCellTypeCopy_actionPerformed(ActionEvent e)
    {
        Cell cellTypeToCopy= (Cell) jComboBoxCellTypes.getSelectedItem();

        logger.logComment("Copying cell: "+ cellTypeToCopy);

        if (cellTypeToCopy == null)
        {
            GuiUtils.showErrorMessage(logger, "Problem copying that Cell Type", null, this);
            return;
        }
        String proposedName = null;
        if (cellTypeToCopy.getInstanceName().indexOf("_")>0)
        {
            int underScoreIndex = cellTypeToCopy.getInstanceName().lastIndexOf("_");
            try
            {
                String val = cellTypeToCopy.getInstanceName().substring(underScoreIndex+1);
                int newVal = Integer.parseInt(val) + 1;
                proposedName
                    = cellTypeToCopy.getInstanceName().substring(0,underScoreIndex)+"_"+newVal;
            }
            catch(NumberFormatException nfe)
            {
                proposedName = cellTypeToCopy.getInstanceName()+"_1";
            }
        }
        else
        {
            proposedName = cellTypeToCopy.getInstanceName()+"_1";
        }
        String newCellName = JOptionPane.showInputDialog(this, "Please enter the name of the new Cell Type", proposedName);


        if (newCellName == null) return; // cancelled...

        Cell newCell = (Cell)cellTypeToCopy.clone();
        newCell.setInstanceName(newCellName);

        try
        {
            projManager.getCurrentProject().cellManager.addCellType(newCell);
        }
        catch (NamingException ex2)
        {
            GuiUtils.showErrorMessage(logger, "Problem with the name of that Cell Type", ex2, this);
            return;
        }
        projManager.getCurrentProject().markProjectAsEdited();

        refreshTabCellTypes();

        jComboBoxCellTypes.setSelectedItem(newCell);
    }

    void jButtonCellTypesMoveToOrigin_actionPerformed(ActionEvent e)
    {
        Cell cellTypeToMove= (Cell) jComboBoxCellTypes.getSelectedItem();

        logger.logComment("Moving to origin cell: "+ cellTypeToMove);

        if (cellTypeToMove == null)
        {
            GuiUtils.showErrorMessage(logger, "Problem moving the Cell Type", null, this);
            return;
        }

        Point3f oldStartPos = cellTypeToMove.getFirstSomaSegment().getStartPointPosition();

        CellTopologyHelper.translateAllPositions(cellTypeToMove,
                                                 new Vector3f(oldStartPos.x*-1,
                                                              oldStartPos.y*-1,
                                                              oldStartPos.z*-1));
        projManager.getCurrentProject().markProjectAsEdited();

        refreshTabCellTypes();

    }

    void jButtonCellTypesConnect_actionPerformed(ActionEvent e)
    {

        Cell cellTypeToConnect= (Cell) jComboBoxCellTypes.getSelectedItem();

        logger.logComment("Reconnecting cell: "+ cellTypeToConnect);

        if (cellTypeToConnect == null || true)
        {
            GuiUtils.showErrorMessage(logger, "Problem reconnecting the Cell Type", null, this);
            return;
        }

        ////////////////////////////CellTopologyHelper.moveSectionsToConnPointsOnParents(cellTypeToConnect);

        projManager.getCurrentProject().markProjectAsEdited();

        refreshTabCellTypes();

    }

    void jButtonCellTypesMakeSimpConn_actionPerformed(ActionEvent e)
    {
        Cell cellType = (Cell) jComboBoxCellTypes.getSelectedItem();

        logger.logComment("Making cell: "+ cellType+" simply connected");

        if (cellType == null)
        {
            GuiUtils.showErrorMessage(logger, "Problem making Cell Type Simply Connected", null, this);
            return;
        }

        boolean result = false;//////////////////CellTopologyHelper.makeSimplyConnected(cellType);

        if(!result)
        {
            GuiUtils.showErrorMessage(logger, "Problem making Cell Type Simply Connected. Note that the cell segments\n"
                                      + "must be properly connected to their parents before completing this step (try\n"
                                      + " pressing \""+jButtonCellTypesConnect.getText()+"\" button)", null, this);
            return;
        }

        projManager.getCurrentProject().markProjectAsEdited();

        refreshTabCellTypes();

    }

    void jButtonNeuroMLExport_actionPerformed(ActionEvent e, String level)
    {

        logger.logComment("saving the cell morphologies in NeuroML form...");

        Vector<Cell> cells = this.projManager.getCurrentProject().cellManager.getAllCells();

                    //GeneralUtils.reorderAlphabetically(cells, true);


        File neuroMLDir = ProjectStructure.getNeuroMLDir(projManager.getCurrentProject().getProjectMainDirectory());


        GeneralUtils.removeAllFiles(neuroMLDir, false, false);

        MorphCompartmentalisation mc = (MorphCompartmentalisation)jComboBoxNeuroMLComps.getSelectedItem();

        for (Cell origCell : cells)
        {
            Cell mappedCell = mc.getCompartmentalisation(origCell);

            File cellFile = null;

            /*   if (!CellTopologyHelper.checkSimplyConnected(cell))
               {
                   GuiUtils.showErrorMessage(logger, "The cell: "+ cell.getInstanceName()
                                             + " is not Simply Connected.\n"
                                             + "This is a currently a requirement for conversion to MorphML format.\n"
             + "Try making a copy of the cell and making it Simply Connected at the Cell Type tab", null, this);
               }
               else
               {*/
            try
            {
                logger.logComment("Cell is of type: " + mappedCell.getClass().getName());
                Cell tempCell = new Cell();
                if (! (mappedCell.getClass().equals(tempCell.getClass())))
                {
                    // This is done because of problems generating MorphML for PurkinjeCell, etc.
                    // These inherit from Cell, but have all their state in the standard constructor.
                    // Saving them in JavaML format would only save the name, and not the segment positions etc.
                    mappedCell = (Cell) mappedCell.clone(); // this produced a copy which is an instance of Cell
                }

                logger.logComment("Saving cell: "
                                  + mappedCell.getInstanceName()
                                  + " in "
                                  + ProjectStructure.getMorphMLFileExtension()
                                  + " format");

                cellFile = new File(ProjectStructure.getNeuroMLDir(projManager.getCurrentProject().getProjectMainDirectory()),
                                    mappedCell.getInstanceName()
                                    + ProjectStructure.getMorphMLFileExtension());

                MorphMLConverter.saveCellInMorphMLFormat(mappedCell, cellFile, level);
                refreshTabNeuroML();
            }
            catch (MorphologyException ex1)
            {
                GuiUtils.showErrorMessage(logger, "Problem saving cell: " + mappedCell.getInstanceName(), ex1, this);
            }
            //}
        }

    }


    void jButtonMorphMLView_actionPerformed(ActionEvent e, boolean formatted)
    {
        String fileToView = (String)jComboBoxNeuroML.getSelectedItem();

        if (fileToView.equals(noNeuroMLFilesFound)) return;

        fileToView = fileToView.substring(0,fileToView.indexOf("(")).trim();

        File file = new File(fileToView);

        if (!formatted)
        {
            SimpleViewer.showFile(fileToView, 12, false, false, this, false, false, "Validate", new ActionListener(){
            public void actionPerformed(ActionEvent e)
            {
                jButtonNeuroMLValidate_actionPerformed(e);
            }
        }
        );
            return;
        }

        SimpleXMLDocument doc = null;
        try
        {
            doc = SimpleXMLReader.getSimpleXMLDoc(file);
        }
        catch (Exception ex)
        {
            GuiUtils.showErrorMessage(logger, "Error showing that XML file", ex, this);
            return;
        }
        //System.out.println("Attrs: "+ doc.getRootElement().getAttributes());
        SimpleViewer.showString( doc.getXMLString("", formatted), "The NeuroML file: "+ fileToView ,12, false, formatted, .9f, .9f,this, false, "Validate", new ActionListener(){
            public void actionPerformed(ActionEvent e)
            {
                jButtonNeuroMLValidate_actionPerformed(e);
            }
        }
        );


    }

    void jButtonGenesisGenerate_actionPerformed(ActionEvent e)
    {
        logger.logComment("Create GENESIS button pressed...");
        doCreateGenesis();

    }

    void jButtonGenesisRun_actionPerformed(ActionEvent e)
    {
        logger.logComment("Run GENESIS button pressed...");
        doRunGenesis();


    }

    void jButtonGenesisView_actionPerformed(ActionEvent e)
    {
        logger.logComment("Viewing a genesis file");

        if (projManager.getCurrentProject() == null)
        {
            logger.logError("No project loaded...");
            return;
        }


            String selected = (String)jComboBoxGenesisFiles.getSelectedItem();

            File selectedFile = new File(ProjectStructure.getGenesisCodeDir(projManager.getCurrentProject().getProjectMainDirectory()),
                                     selected);

            logger.logComment("Viewing genesis file: "+selectedFile);

            SimpleViewer.showFile(selectedFile.getAbsolutePath(), 12, false, false, true);


    }

    void jButtonRegionsEdit_actionPerformed(ActionEvent e)
    {
        logger.logComment("Editing selected region");
        doEditRegion();
    }

    void jMenuItemCondorMonitor_actionPerformed(ActionEvent e)
    {
        logger.logComment("CondorMonitor to run...");

        new CondorApp(false);

    }


    void jMenuItemPlotEquation_actionPerformed(ActionEvent e)
    {
        logger.logComment("jMenuItemPlotEquation_actionPerformed...");

        DataSet ds = PlotterFrame.addManualPlot(100, 0, 10, this);

        PlotterFrame frame = PlotManager.getPlotterFrame("New Data Set plot", false, false);

        frame.addDataSet(ds);

        frame.setVisible(true);

    }



    void jMenuItemPlotImport_actionPerformed(ActionEvent e)
    {
        logger.logComment("jMenuItemPlotImport_actionPerformed...");

        String lastDir = recentFiles.getMyLastExportPointsDir();

        if (lastDir == null) lastDir
            = projManager.getCurrentProject().getProjectMainDirectory().getAbsolutePath();

        File defaultDir = new File(lastDir);

        DataSet ds = PlotterFrame.addNewDataSet(defaultDir, this);

        PlotterFrame frame = PlotManager.getPlotterFrame("Imported Data Set plot", false, false);

        if (ds != null)
        {
            frame.addDataSet(ds);
            frame.setVisible(true);
        }
    }



    void jMenuItemMPIMonitor_actionPerformed(ActionEvent e)
    {
        logger.logComment("MPIMonitor to run...");

        MpiFrame mpiFrame = new MpiFrame(this.projManager.getCurrentProject().getProjectMainDirectory(), false);

        GuiUtils.centreWindow(mpiFrame);

        mpiFrame.setVisible(true);

    }


    void jButtonNeuronCreateCondor_actionPerformed(ActionEvent e)
    {
        logger.logComment("Creating Condor ready hoc");
        doCreateHoc(NeuronFileManager.RUN_VIA_CONDOR);
    }

    void jButtonNeuronCreateMPI_actionPerformed(ActionEvent e)
    {
        logger.logComment("Creating Parallel ready hoc");
        doCreateHoc(NeuronFileManager.RUN_PARALLEL);
    }





    void jButton3DPrevSims_actionPerformed(ActionEvent e)
    {
        logger.logComment("Loading a previous simulation...");
        doViewPrevSimulations();

    }

    void doViewPrevSimulations()
    {
        this.doDestroy3D();
        File simDir = ProjectStructure.getSimulationsDir(projManager.getCurrentProject().getProjectMainDirectory());

        SimulationBrowser dlg = new SimulationBrowser(simDir, this);
        Dimension dlgSize = dlg.getPreferredSize();
        Dimension frmSize = getSize();
        Point loc = getLocation();
        dlg.setLocation( (frmSize.width - dlgSize.width) / 2 + loc.x,
                        (frmSize.height - dlgSize.height) / 2 + loc.y);
        dlg.setModal(true);
        dlg.pack();
        dlg.setVisible(true);

        SimulationData selected  =  null;

        if (dlg.cancelled)
        {
            logger.logComment("User cancelled...");
            return;
        }

        try
        {
            selected = dlg.getSelectedSimulation();
        }
        catch (SimulationDataException ex)
        {
            GuiUtils.showErrorMessage(logger, "There was a problem loading that simulation", ex, this);
            return;
        }
        doCreate3D(selected.getSimulationDirectory());

        logger.logComment("Selected sim: "+ selected);

        this.jEditorPaneGenerateInfo.setText("<p>Network has been reloaded from simulation: <b>"+selected+"</b></p>"+"<br>"
                                             +"<center><b>Cell Groups:</b></center>"
                                             +projManager.getCurrentProject().generatedCellPositions.getHtmlReport()+"<br>"
                                            +"<center><b>Network Connections:</b></center>"

            +projManager.getCurrentProject().generatedNetworkConnections.getHtmlReport(GeneratedNetworkConnections.ANY_NETWORK_CONNECTION,
            null)+"");

        this.refreshTabGenerate();

    }



/*
    void jButtonStimWhereToStim_actionPerformed(ActionEvent e)
    {
        if (jRadioButtonStimNone.isSelected())
        {
            GuiUtils.showErrorMessage(logger, "Please select the type of stimulation above first", null, this);
            return;
        }
        String cellGroupToStim = projManager.getCurrentProject().stimulationSettings.cellGroup;

        String cellType = projManager.getCurrentProject().cellGroupsInfo.getCellType(cellGroupToStim);
        Cell cellForSelectedGroup = projManager.getCurrentProject().cellManager.getCellType(cellType);

        SegmentSelector dlg = new SegmentSelector(this, cellForSelectedGroup, false, true);

        Vector segments = cellForSelectedGroup.getAllSegments();
        Segment segToStim = (Segment)segments.elementAt(projManager.getCurrentProject().stimulationSettings.segmentID);

        dlg.setSelectedSegment(segToStim);


//Center the window
        Dimension screenSize = Toolkit.getDefaultToolkit().getScreenSize();
        Dimension frameSize = dlg.getSize();
        if (frameSize.height > screenSize.height)
        {
            frameSize.height = screenSize.height;
        }
        if (frameSize.width > screenSize.width)
        {
            frameSize.width = screenSize.width;
        }
        dlg.setLocation( (screenSize.width - frameSize.width) / 2, (screenSize.height - frameSize.height) / 2);
        dlg.show();

        //dlg.setSelectedSegment( (Segment) p.getAllSegments().elementAt(1));

        if(dlg.cancelled) return;

        if (dlg.getSelectedSegment()==null) return;

        projManager.getCurrentProject().markProjectAsEdited();

        projManager.getCurrentProject().stimulationSettings.setSegmentID(dlg.getSelectedSegment().getSegmentId());

        jTextFieldStimWhere.setText(dlg.getSelectedSegment().getSegmentName()
                                    + " (ID: "
                                    +dlg.getSelectedSegment().getSegmentId()
                                    + ")");

    }
*/


    void jButtonMechanismAdd_actionPerformed(ActionEvent e)
    {
        logger.logComment("Adding a new cell Mechanism...");

        GuiUtils.showWarningMessage(logger, "Note: use of this type of Cell Mechanism is not advised. Use a ChannelML Mechanism instead.", this);

        CellMechanismEditor cellProcEditor = new CellMechanismEditor(projManager.getCurrentProject(), this);

        cellProcEditor.pack();
        cellProcEditor.setVisible(true);

        /*
        if (cellProcEditor.cancelled) return;

        CellProcess suggestedCellProc = cellProcEditor.getFinalCellProcess();

        while (projManager.getCurrentProject().cellProcessInfo.getAllCellProcessNames().contains(suggestedCellProc.getInstanceName()))
        {
            GuiUtils.showErrorMessage(logger, "That name: "+suggestedCellProc.getInstanceName()+" has already been used. Please select another.", null, this);
            logger.logComment("Reshowing the dialog...");
            cellProcEditor.setVisible(true);
           if (cellProcEditor.cancelled) return;
           suggestedCellProc = cellProcEditor.getFinalCellProcess();
        }

        projManager.getCurrentProject().cellProcessInfo.addCellProcess(cellProcEditor.getFinalCellProcess());
*/

    }

    void jButtonMechanismEdit_actionPerformed(ActionEvent e)
    {
        logger.logComment("----------------------------         Editing a cell mechanism...");

        int selectedRow = jTableMechanisms.getSelectedRow();

        if (selectedRow < 0)
        {
            logger.logComment("No row selected...");
            return;
        }
        CellMechanism cellMech = projManager.getCurrentProject().cellMechanismInfo.getCellMechanismAt(selectedRow);


        if (cellMech instanceof AbstractedCellMechanism)
        {

            logger.logComment("Cell mechanism:");
            ((AbstractedCellMechanism)cellMech).printDetails();

            CellMechanismEditor cellMechEditor = new CellMechanismEditor(projManager.getCurrentProject(), this);

            cellMechEditor.setCellMechanism((AbstractedCellMechanism)cellMech);

            cellMechEditor.pack();
            cellMechEditor.setVisible(true);
            if (cellMechEditor.cancelled)return;

            AbstractedCellMechanism cp = cellMechEditor.getFinalCellMechanism();
            cp.printDetails();

            projManager.getCurrentProject().cellMechanismInfo.updateCellMechanism(cp);
        }
        else if (cellMech instanceof ChannelMLCellMechanism)
        {
            ChannelMLEditor cmlEditor
                = new ChannelMLEditor( (ChannelMLCellMechanism) cellMech,
                                      projManager.getCurrentProject(),
                                      this);

            Dimension screenSize = Toolkit.getDefaultToolkit().getScreenSize();

            Dimension frameSize = cmlEditor.getSize();
            if (frameSize.height > screenSize.height) frameSize.height = screenSize.height;
            if (frameSize.width > screenSize.width) frameSize.width = screenSize.width;
            cmlEditor.setLocation( (screenSize.width - frameSize.width) / 2, (screenSize.height - frameSize.height) / 2);

            cmlEditor.setVisible(true);

            //System.out.println("Shown the dialog");


        }
    }



    void jButtonMechanismDelete_actionPerformed(ActionEvent e)
    {
        logger.logComment("Deleting a cell mech...");
        int selectedRow = jTableMechanisms.getSelectedRow();

        if (selectedRow < 0)
        {
            logger.logComment("No row selected...");
            return;
        }
        CellMechanism cellMech = projManager.getCurrentProject().cellMechanismInfo.getCellMechanismAt(selectedRow);


        File dirForFiles = ProjectStructure.getDirForCellMechFiles(projManager.getCurrentProject(), cellMech.getInstanceName());
        GeneralUtils.removeAllFiles(dirForFiles, true, true);


        projManager.getCurrentProject().cellMechanismInfo.deleteCellMechanism(cellMech);

        projManager.getCurrentProject().markProjectAsEdited();

    }

    void jButtonMechanismFileBased_actionPerformed(ActionEvent e)
    {
        String cellMechanismName = JOptionPane.showInputDialog(this,
                                    "Please enter the name of the new Mechanism",
                                    "New File Based Cell Mechanism",
                                    JOptionPane.QUESTION_MESSAGE);

        logger.logComment("Input: "+ cellMechanismName);

        if (cellMechanismName==null || cellMechanismName.trim().length()==0)
        {
            logger.logError("No cellMechanismName inputted...");
            return;
        }

        if (cellMechanismName.indexOf(" ")>=0 ||
            cellMechanismName.indexOf(".")>=0 ||
            cellMechanismName.indexOf(",")>=0 ||
            cellMechanismName.indexOf("-")>=0 ||
            cellMechanismName.indexOf("/")>=0 ||
            cellMechanismName.indexOf("\\")>=0)
        {
            GuiUtils.showErrorMessage(logger,
                                      "Please type a Cell Mechanism name with just "
                                      +"letters and digits and underscores, no spaces", null, this);
            jButtonMechanismFileBased_actionPerformed(e);
        }

        if (this.projManager.getCurrentProject().cellMechanismInfo.getAllCellMechanismNames().contains(cellMechanismName))
        {
            GuiUtils.showErrorMessage(logger,
                                      "The Cell Mechanism name: "+cellMechanismName+" has already been taken", null, this);
            jButtonMechanismFileBased_actionPerformed(e);


        }


        FileBasedMembraneMechanism fmp = new FileBasedMembraneMechanism();

        fmp.setInstanceName(cellMechanismName);


        Object[] options = {AbstractedCellMechanism.CHANNEL_MECHANISM,
            AbstractedCellMechanism.SYNAPTIC_MECHANISM,
            AbstractedCellMechanism.ION_CONCENTRATION};

        JOptionPane option = new JOptionPane("Please select the type of Cell Mechanism",
                                             JOptionPane.DEFAULT_OPTION,
                                             JOptionPane.PLAIN_MESSAGE,
                                             null,
                                             options,
                                             options[0]);

        JDialog dialog = option.createDialog(this, "Select type of Cell Mechanism");
        dialog.setVisible(true);

        Object choice = option.getValue();
        logger.logComment("User has chosen: " + choice);

        this.projManager.getCurrentProject().markProjectAsEdited();

        fmp.specifyMechanismType( (String) choice);

        String[] simEnvs
            = new String[]
            {SimEnvHelper.NEURON,
            SimEnvHelper.GENESIS};


        //File proposedLoc = ProjectStructure.getFileBasedCellProcessesDir(projManager.getCurrentProject().getProjectMainDirectory(), true);

        File proposedLoc = ProjectStructure.getDirForCellMechFiles(projManager.getCurrentProject(), cellMechanismName);

        for (int i = 0; i < simEnvs.length; i++)
        {

            int proceed = JOptionPane.showConfirmDialog(this,
                 "Will there be a "+simEnvs[i]+" implementation of this Cell Mechanism?\n"
                 + "(Selected file will be imported into project in directory "
                 + proposedLoc.getAbsolutePath() + ")\n\n"
                 +"Note that the file needs to have the name of the process replaced by "
                 + MechanismImplementation.getNamePlaceholder()
                 + "\nSee examples in the templates/ directory.",
                 "New File Based Cell Mechanism Implementation",
                 JOptionPane.YES_NO_OPTION);

            logger.logComment("Input: " + proceed);

            if(proceed == JOptionPane.YES_OPTION)
            {

                JFileChooser chooser = new JFileChooser();
                chooser.setDialogType(JFileChooser.OPEN_DIALOG);

                String lastCellMechDir = recentFiles.getMyLastCellProcessesDir();

                if (lastCellMechDir == null) lastCellMechDir
                    = ProjectStructure.getnCProjectsDirectory().getAbsolutePath();

                File defaultDir = new File(lastCellMechDir);

                chooser.setCurrentDirectory(defaultDir);
                logger.logComment("Set Dialog dir to: " + defaultDir.getAbsolutePath());
                chooser.setDialogTitle("Choose "+simEnvs[i]+" file for Cell Mechanism: "
                        + cellMechanismName);
                int retval = chooser.showDialog(this, "Choose "+simEnvs[i]+" file");

                if (retval == JOptionPane.OK_OPTION)
                {

                    File newFile = null;
                    try
                    {
                        newFile = GeneralUtils.copyFileIntoDir(new File(chooser.getSelectedFile().getAbsolutePath()),
                                                        proposedLoc);

                        recentFiles.setMyLastCellProcessesDir(chooser.getSelectedFile().getParent());
                    }
                    catch (IOException ex)
                    {
                        GuiUtils.showErrorMessage(logger, "Problem when including new Cell Mechanism", ex, this);
                        return;
                    }


                    fmp.specifyNewImplFile(simEnvs[i],
                                           newFile.getName());
                }
            }

        }


        projManager.getCurrentProject().cellMechanismInfo.addCellMechanism(fmp);
    }



    void jButtonMechanismNewCML_actionPerformed(ActionEvent e)
    {
        String cellMechanismName = JOptionPane.showInputDialog(this,
                                    "Please enter the name of the new ChannelML based Mechanism",
                                    "New ChannelML Cell Mechanism",
                                    JOptionPane.QUESTION_MESSAGE);

        logger.logComment("Input: "+ cellMechanismName);

        //File cellMechDir = ProjectStructure.getCellProcessesDir(projManager.getCurrentProject().getProjectMainDirectory());


        if (cellMechanismName==null || cellMechanismName.trim().length()==0)
        {
            logger.logError("No cellMechanismName inputted...");
            return;
        }

        if (cellMechanismName.indexOf(" ")>=0)
        {
            GuiUtils.showErrorMessage(logger, "Please type a Cell Mechanism name without spaces", null, this);
            jButtonMechanismNewCML_actionPerformed(e);
            return;
        }

        File dirForCMLFiles = ProjectStructure.getDirForCellMechFiles(projManager.getCurrentProject(), cellMechanismName);

        if (dirForCMLFiles.exists())
        {
            GuiUtils.showErrorMessage(logger, "The Cell Mechanism name: "+ cellMechanismName +" is already being used.", null, this);
            jButtonMechanismNewCML_actionPerformed(e);
            return;

        }
        dirForCMLFiles.mkdir();


        ChannelMLCellMechanism cmlm = new ChannelMLCellMechanism();

        cmlm.setInstanceName(cellMechanismName);



        Object[] options = {CellMechanism.CHANNEL_MECHANISM,
                            CellMechanism.SYNAPTIC_MECHANISM};

        JOptionPane option = new JOptionPane("Please select the type of Cell Mechanism",
                                             JOptionPane.DEFAULT_OPTION,
                                             JOptionPane.PLAIN_MESSAGE,
                                             null,
                                             options,
                                             options[0]);

        JDialog dialog = option.createDialog(this, "Select type of Cell Mechanism");
        dialog.setVisible(true);

        Object choice = option.getValue();
        logger.logComment("User has chosen: " + choice);

        cmlm.setMechanismType( (String) choice);


        JFileChooser cmlFileChooser = new JFileChooser();

        String lastCellMechDir = recentFiles.getMyLastCellProcessesDir();

        if (lastCellMechDir == null) lastCellMechDir
            = ProjectStructure.getCMLExamplesDir().getAbsolutePath();

        //System.out.println("lastCellMechDir: " + lastCellMechDir);

        File defaultDir = new File(lastCellMechDir);

        cmlFileChooser.setCurrentDirectory(defaultDir);


        cmlFileChooser.setDialogType(JFileChooser.OPEN_DIALOG);


        cmlFileChooser.setDialogTitle("Choose ChannelML (*.xml) file");
        int retvalCML = cmlFileChooser.showDialog(this, "Choose ChannelML file for Cell Mechanism: " + cellMechanismName);
        File cmlFile = null;

        if (retvalCML == JOptionPane.OK_OPTION)
        {
            try
            {
                cmlFile = GeneralUtils.copyFileIntoDir(cmlFileChooser.getSelectedFile(),
                                                              dirForCMLFiles);

                cmlm.setChannelMLFile(cmlFileChooser.getSelectedFile().getName());

                //cmlm.initialise(this.projManager.getCurrentProject(), true);

            }
            catch (IOException ex1)
            {
                GuiUtils.showErrorMessage(logger, "Problem copying the Cell Mechanism file: "+ cmlFileChooser.getSelectedFile() +
                                          " into the project at: "+ dirForCMLFiles, null, this);
                return;

            }


            recentFiles.setMyLastCellProcessesDir(cmlFileChooser.getSelectedFile().getParent());
        }


        cmlm.setMechanismModel("ChannelML based process");
        cmlm.setDescription("Cell Mechanism based on ChannelML file: "+ cmlFile.getName());



        String[] simEnvs
            = new String[]
            {SimEnvHelper.NEURON,
            SimEnvHelper.GENESIS};

        for (int i = 0; i < simEnvs.length; i++)
        {

            int proceed = JOptionPane.showConfirmDialog(this,
                                                        "Is there a mapping (*.xsl) of this Cell Mechanism to "+ simEnvs[i] +
                                                        "?\n\n"
                                                        +"Note that the latest XSL mappings for NEURON and GENESIS "
                                                        +"can be found in:\n" +ProjectStructure.getCMLTemplatesDir().getAbsolutePath() +"",
                                                        "Mapping to "+ simEnvs[i]+"?",
                                                        JOptionPane.YES_NO_OPTION);

            logger.logComment("Input: " + proceed);

            if (proceed == JOptionPane.YES_OPTION)
            {

                JFileChooser chooser = new JFileChooser();
                chooser.setDialogType(JFileChooser.OPEN_DIALOG);

                String lastCellMappingDir = ProjectStructure.getCMLSchemasDir().getAbsolutePath();

                defaultDir = new File(lastCellMappingDir);

                chooser.setCurrentDirectory(defaultDir);
                logger.logComment("Set Dialog dir to: " + defaultDir.getAbsolutePath());
                chooser.setDialogTitle("Choose " + simEnvs[i] + " mapping file for Cell Mechanism: " + cellMechanismName);
                int retval = chooser.showDialog(this, "Choose " + simEnvs[i] + " file");

                if (retval == JOptionPane.OK_OPTION)
                {
                    //File cpDir = ProjectStructure.getCellProcessesDir(projManager.getCurrentProject().getProjectMainDirectory());

                    //File newLocation = new File(cpDir, cmlm.getInstanceName());

                    File newFile = null;
                    try
                    {
                        newFile = GeneralUtils.copyFileIntoDir(chooser.getSelectedFile(),
                                                               dirForCMLFiles);

                        recentFiles.setMyLastCellProcessesDir(chooser.getSelectedFile().getParent());
                    }
                    catch (IOException ex)
                    {
                        GuiUtils.showErrorMessage(logger, "Problem when including new Cell Mechanism", ex, this);
                        return;
                    }

                    /** @todo Put in dialog asking if it's a mod mapping for neuron, and set real val for requiresCompilation */

                    SimXSLMapping sxm = new SimXSLMapping(newFile.getName(), simEnvs[i], true);
                    cmlm.addSimMapping(sxm);


                }
            }

        }


        try
        {
            // This is to make sure the files have been copied into the correct dirs..
            Thread.sleep(1000);
        }
        catch (InterruptedException ex)
        {
            ex.printStackTrace();
        }


        projManager.getCurrentProject().cellMechanismInfo.addCellMechanism(cmlm);


        try
        {
            cmlm.initialise(projManager.getCurrentProject(), true);
        }
        catch (ChannelMLException ex1)
        {
            GuiUtils.showErrorMessage(logger,
                                      "Error initialising Cell Mechanism: " +cmlm.getInstanceName() +", "+
                                      ex1.getMessage(),
                                      ex1,
                                      null);

        }


        ChannelMLEditor frame = new ChannelMLEditor(cmlm, projManager.getCurrentProject(), this);

        Dimension screenSize = Toolkit.getDefaultToolkit().getScreenSize();

        Dimension frameSize = frame.getSize();
        if (frameSize.height > screenSize.height) frameSize.height = screenSize.height;
        if (frameSize.width > screenSize.width) frameSize.width = screenSize.width;
        frame.setLocation( (screenSize.width - frameSize.width) / 2, (screenSize.height - frameSize.height) / 2);

        frame.setVisible(true);

        this.projManager.getCurrentProject().markProjectAsEdited();

        //System.out.println("Shown the dialog");

    }



    void jButtonMechanismTemplateCML_actionPerformed(ActionEvent e)
    {

        File cmlTemplateDir = ProjectStructure.getCMLTemplatesDir();

        File[] dirs = cmlTemplateDir.listFiles();

        Vector<String> possibilities = new Vector<String>();

        for (int i = 0; i < dirs.length; i++)
        {
            if (dirs[i].isDirectory() && !GeneralUtils.isVersionControlDir(dirs[i]) && !dirs[i].getName().equals("old"))
            {
                String name = dirs[i].getName();
                Properties props = new Properties();
                try
                {
                    props.load(new FileInputStream(new File(dirs[i], "properties")));
                    name = name + ": " + props.getProperty("Description");
                }
                catch (IOException ex)
                {
                    logger.logError("Problem getting properties", ex);
                    //ignore...
                }

                possibilities.add(name);
            }
        }

        String chosen = (String)JOptionPane.showInputDialog(this,
                    "Please select the ChannelML template for the new Cell Mechanism",
                    "Choose template",
                    JOptionPane.PLAIN_MESSAGE,
                    null,
                    possibilities.toArray(),
                    possibilities.firstElement());

        if (chosen == null)
        {
            logger.logComment("Null chosen. user cancelled...");
            return;
        }
        if (chosen.indexOf(":")>0 )  // trim description
            chosen = chosen.substring(0,chosen.indexOf(":"));


        File fromDir = new File(cmlTemplateDir, chosen);

        Properties props = new Properties();

        try
        {
            props.load(new FileInputStream(new File(fromDir, "properties")));
        }
        catch (IOException ex)
        {
            logger.logError("Problem getting properties", ex);
            //ignore...
        }


        boolean goodName = false;

        File dirForCMLFiles = null;

        String propCellMechanismName = chosen;
        if (props.getProperty("DefaultName") != null)
            propCellMechanismName = props.getProperty("DefaultName");

        Vector allCellProcs = this.projManager.getCurrentProject().cellMechanismInfo.getAllCellMechanismNames();

        while (!goodName)
        {
            if (allCellProcs.contains(propCellMechanismName))
            {
                if (propCellMechanismName.indexOf("_")>0)
                {
                    String num = propCellMechanismName.substring(propCellMechanismName.lastIndexOf("_")+1);
                    try
                    {
                        int nextNum = Integer.parseInt(num) +1;

                        propCellMechanismName
                            = propCellMechanismName.substring(0, propCellMechanismName.lastIndexOf("_"))
                            + "_" + nextNum;
                    }
                    catch (NumberFormatException nfe)
                    {
                        propCellMechanismName = propCellMechanismName + "_1";
                    }
                }
                else
                {
                    propCellMechanismName = propCellMechanismName +"_1";
                }
            }

             propCellMechanismName = JOptionPane.showInputDialog(this, "Please enter the name of the new ChannelML based Mechanism",
                                                               propCellMechanismName);

            goodName = true;

            logger.logComment("Input: " + propCellMechanismName);

            //File cellProcDir = ProjectStructure.getCellProcessesDir(projManager.getCurrentProject().getProjectMainDirectory());

            if (propCellMechanismName == null || propCellMechanismName.trim().length() == 0)
            {
                logger.logError("No cellMechanismName inputted...");
                return;
            }

            if (propCellMechanismName.indexOf(" ") >= 0)
            {
                GuiUtils.showErrorMessage(logger, "Please type a Cell Mechanism name without spaces", null, this);
                goodName = false;
            }

            dirForCMLFiles = ProjectStructure.getDirForCellMechFiles(projManager.getCurrentProject(), propCellMechanismName);

            if (dirForCMLFiles.exists() || allCellProcs.contains(propCellMechanismName))
            {
                GuiUtils.showErrorMessage(logger, "The Cell Mechanism name: " + propCellMechanismName + " is already being used.", null, this);
                goodName = false;

            }
        }

        dirForCMLFiles.mkdir();

         ChannelMLCellMechanism cmlMech = new ChannelMLCellMechanism();

         cmlMech.setInstanceName(propCellMechanismName);

        if (props.getProperty("CellProcessType")!=null)
        {
            cmlMech.setMechanismType(props.getProperty("CellProcessType"));
        }
        else
        {

            Object[] options =
                {CellMechanism.CHANNEL_MECHANISM,
                CellMechanism.SYNAPTIC_MECHANISM};

            JOptionPane option = new JOptionPane("Please select the type of Cell Mechanism",
                                                 JOptionPane.DEFAULT_OPTION,
                                                 JOptionPane.PLAIN_MESSAGE,
                                                 null,
                                                 options,
                                                 options[0]);

            JDialog dialog = option.createDialog(this, "Select type of Cell Mechanism");
            dialog.setVisible(true);

            Object choice = option.getValue();
            logger.logComment("User has chosen: " + choice);

            cmlMech.setMechanismType( (String) choice);
        }


        logger.logComment("Checking files in: "+ fromDir);

        File[] contents = fromDir.listFiles();
        try
        {
            if (props.getProperty("ChannelMLFile")!=null)
            {
                String relativeFile = props.getProperty("ChannelMLFile");

                File absFile = new File(fromDir, relativeFile);
                absFile = absFile.getCanonicalFile();

                logger.logComment("ChannelML file found in props to be: "+ absFile);

                File newFile = GeneralUtils.copyFileIntoDir(absFile, dirForCMLFiles);
                cmlMech.setChannelMLFile(newFile.getName());
            }
            if (props.getProperty("MappingNEURON")!=null)
            {
                String relativeFile = props.getProperty("MappingNEURON");

                File absFile = new File(fromDir, relativeFile);
                absFile = absFile.getCanonicalFile();

                logger.logComment("MappingNEURON file found in props to be: "+ absFile);

                File newFile = GeneralUtils.copyFileIntoDir(absFile, dirForCMLFiles);

                SimXSLMapping mapping = new SimXSLMapping(newFile.getName(),
                                                          SimEnvHelper.NEURON, true); // can be reset later

                cmlMech.addSimMapping(mapping);

            }

            if (props.getProperty("MappingGENESIS")!=null)
            {
                String relativeFile = props.getProperty("MappingGENESIS");

                File absFile = new File(fromDir, relativeFile);
                absFile = absFile.getCanonicalFile();

                logger.logComment("MappingGENESIS file found in props to be: "+ absFile);

                File newFile = GeneralUtils.copyFileIntoDir(absFile, dirForCMLFiles);

                SimXSLMapping mapping = new SimXSLMapping(newFile.getName(),
                                                          SimEnvHelper.GENESIS, false);

                cmlMech.addSimMapping(mapping);

            }



            // If not found by the properties, look in the dir itself...

            for (int i = 0; i < contents.length; i++)
            {
                if (props.getProperty("ChannelMLFile")==null &&
                    contents[i].getName().endsWith(ChannelMLConstants.DEFAULT_FILE_EXTENSION))
                {
                    File newFile = GeneralUtils.copyFileIntoDir(contents[i], dirForCMLFiles);
                    cmlMech.setChannelMLFile(newFile.getName());

                }
                if (contents[i].getName().endsWith(ChannelMLConstants.DEFAULT_MAPPING_EXTENSION))
                {
                    File newFile = GeneralUtils.copyFileIntoDir(contents[i], dirForCMLFiles);
                    //cmlMech.setChannelMLFile(newFile.getName());

                    if (props.getProperty("MappingNEURON")==null &&
                        newFile.getName().startsWith(SimEnvHelper.NEURON))
                    {
                        SimXSLMapping mapping = new SimXSLMapping(newFile.getName(),
                                                                  SimEnvHelper.NEURON, true); // true can be reset later

                        cmlMech.addSimMapping(mapping);
                    }
                    if (props.getProperty("MappingGENESIS")==null &&
                        newFile.getName().startsWith(SimEnvHelper.GENESIS))
                    {
                        SimXSLMapping mapping = new SimXSLMapping(newFile.getName(),
                                                                  SimEnvHelper.GENESIS, false);

                        cmlMech.addSimMapping(mapping);
                    }
                }
            }

            //cmlMech.initialise(projManager.getCurrentProject());

            if (cmlMech.getChannelMLFile()==null)
            {
                GuiUtils.showErrorMessage(logger,
                                          "Error finding the "
                                          +ChannelMLConstants.DEFAULT_FILE_EXTENSION
                                          +" file for the Cell Mechanism: " + propCellMechanismName, null, this);

                return;
            }

            if (props.getProperty("NEURONNeedsCompilation") != null)
            {
                Boolean b = Boolean.parseBoolean(props.getProperty("NEURONNeedsCompilation"));
                cmlMech.getSimMapping(SimEnvHelper.NEURON).setRequiresCompilation(b.booleanValue());
            }

            if (props.getProperty("Description") != null)
            {
                cmlMech.setDescription(props.getProperty("Description"));
            }
            else
            {
                cmlMech.setDescription("Template based ChannelML file");
            }
            cmlMech.setMechanismModel("Template based ChannelML file");

            projManager.getCurrentProject().cellMechanismInfo.addCellMechanism(cmlMech);

            logger.logComment("Added CML process with main file: "+ cmlMech.getChannelMLFile());




            try
            {
                // This is to make sure the files have been copied into the correct dirs..
                Thread.sleep(1000);
            }
            catch (InterruptedException ex)
            {
                ex.printStackTrace();
            }

            System.out.println("Chan mech: "+cmlMech.toString());

            cmlMech.initialise(projManager.getCurrentProject(), true);

            try
            {
                if (cmlMech.isPassiveNonSpecificCond())
                {
                    double revPot = projManager.getCurrentProject().simulationParameters.
                        getGlobalVLeak();

                    double condDens = 1 /
                        this.projManager.getCurrentProject().simulationParameters.getGlobalRm();

                    if (cmlMech.getUnitsUsedInFile().equals(ChannelMLConstants.SI_UNITS))
                    {
                        revPot = UnitConverter.getVoltage(revPot, UnitConverter.NEUROCONSTRUCT_UNITS, UnitConverter.GENESIS_SI_UNITS);
                        condDens = UnitConverter.getConductanceDensity(condDens, UnitConverter.NEUROCONSTRUCT_UNITS, UnitConverter.GENESIS_SI_UNITS);
                    }
                    else if (cmlMech.getUnitsUsedInFile().equals(ChannelMLConstants.PHYSIOLOGICAL_UNITS))
                    {
                        revPot = UnitConverter.getVoltage(revPot, UnitConverter.NEUROCONSTRUCT_UNITS, UnitConverter.GENESIS_PHYSIOLOGICAL_UNITS);
                        condDens = UnitConverter.getConductanceDensity(condDens, UnitConverter.NEUROCONSTRUCT_UNITS, UnitConverter.GENESIS_PHYSIOLOGICAL_UNITS);
                    }


                    cmlMech.getXMLDoc().setValueByXPath(ChannelMLConstants.getIonRevPotXPath(1),
                                                        revPot + "");
                    cmlMech.getXMLDoc().setValueByXPath(ChannelMLConstants.getCondDensXPath(),
                                                        condDens + "");

                }
            }
            catch (Exception ex)
            {
                GuiUtils.showErrorMessage(logger,
                    "Error setting the default membrane resistance and leak potential on that passive conductance", ex, this);
            }



            ChannelMLEditor frame = new ChannelMLEditor(cmlMech, projManager.getCurrentProject(), this);

            Dimension screenSize = Toolkit.getDefaultToolkit().getScreenSize();

            Dimension frameSize = frame.getSize();
            if (frameSize.height > screenSize.height) frameSize.height = screenSize.height;
            if (frameSize.width > screenSize.width) frameSize.width = screenSize.width;
            frame.setLocation( (screenSize.width - frameSize.width) / 2, (screenSize.height - frameSize.height) / 2);


            cmlMech.saveCurrentState(projManager.getCurrentProject());

            frame.setVisible(true);

            this.projManager.getCurrentProject().markProjectAsEdited();

            //System.out.println("Shown the dialog");

        }
        catch (IOException ex)
        {
            GuiUtils.showErrorMessage(logger,
                                      "Problem adding the Cell Mechanism from "+ fromDir.getAbsolutePath()
                                      +" into the project",
                                      ex, this);
            return;
        }
        catch (ChannelMLException ex)
        {
            GuiUtils.showErrorMessage(logger,
                                      "Problem adding the Cell Mechanism from "+ fromDir.getAbsolutePath()
                                      +" into the project",
                                      ex, this);
            return;
        }



    }

    void jButtonGenesisNumMethod_actionPerformed(ActionEvent e)
    {
        String request = "Please enter the new numerical integration method (-1, 0, 2, 3, 4, 5, 10 or 11)";

        String inputValue = JOptionPane.showInputDialog(this,
                                                        request,
                                                        projManager.getCurrentProject().genesisSettings.getNumMethod().getMethodNumber()+"");
        if (inputValue==null) return;
        try
        {
            int newNumMeth = Integer.parseInt(inputValue);
            projManager.getCurrentProject().genesisSettings.getNumMethod().setMethodNumber(newNumMeth);
        }
        catch (NumberFormatException ex)
        {
            GuiUtils.showErrorMessage(logger, "Please enter a valid numerical integration method number (-1, 0, 2, 3, 4, 5, 10 or 11)", ex, this);
            return;
        }

        if (projManager.getCurrentProject().genesisSettings.getNumMethod().getMethodNumber() ==10
            || projManager.getCurrentProject().genesisSettings.getNumMethod().getMethodNumber() ==11)
        {

            projManager.getCurrentProject().genesisSettings.getNumMethod().setHsolve(true);

            request = "Assuming use of hsolve. Please enter the chanmode (0 to 3 inclusive)";

            inputValue = JOptionPane.showInputDialog(this,
                                                            request,
                                                            projManager.getCurrentProject().genesisSettings.getNumMethod().
                                                            getChanMode() + "");
            if (inputValue == null)return;
            try
            {
                int newNumMeth = Integer.parseInt(inputValue);
                projManager.getCurrentProject().genesisSettings.getNumMethod().setChanMode(newNumMeth);
            }
            catch (NumberFormatException ex)
            {
                GuiUtils.showErrorMessage(logger, "Please enter a valid chanmode number", ex, this);
                return;
            }
        }
        else
        {
            projManager.getCurrentProject().genesisSettings.getNumMethod().setHsolve(false);
        }

        projManager.getCurrentProject().markProjectAsEdited();
        refreshTabGenesis();
        //
    }

    void jMenuItemCopyProject_actionPerformed(ActionEvent e)
    {
        if (!checkToSave()) return;

        doSaveAs();

    }

    public void doSaveAs()
    {

        String oldProjName = projManager.getCurrentProject().getProjectName();
        File oldProjFile = projManager.getCurrentProject().getProjectFile();

        logger.logComment("Creating copy of project...");


        JFileChooser chooser = new JFileChooser();
        chooser.setDialogType(JFileChooser.OPEN_DIALOG);
        chooser.setFileSelectionMode(JFileChooser.DIRECTORIES_ONLY);
        chooser.setDialogTitle("Choose directory for copy of project");

        try
        {
            chooser.setCurrentDirectory(ProjectStructure.getnCProjectsDirectory());
        }
        catch (Exception ex)
        {
            logger.logError("Problem with default dir setting: "
                            + projManager.getCurrentProject().getProjectMainDirectory(), ex);
        }


        int retval = chooser.showDialog(this, "Select");

        if (retval != JFileChooser.APPROVE_OPTION)
        {
            logger.logComment("User cancelled...");

            return;
        }



        String newProjectName = JOptionPane.showInputDialog(this,
                                    "Please enter the name of the new project",
                                    oldProjName+"_copy");


        File newProjdir = new File(chooser.getSelectedFile(), newProjectName);



        if (newProjdir.exists())
        {
            GuiUtils.showErrorMessage(logger, "The file " + newProjdir
                                      + " already exists. Please use another name for the copy of this project", null, this);
            return;
        }

        this.closeProject();

        newProjdir.mkdir();

        File newProjectFile = null;

        try
        {
            File tempProjectFile = GeneralUtils.copyFileIntoDir(oldProjFile, newProjdir);

            newProjectFile  = new File(tempProjectFile.getParentFile(),
                                       newProjectName
                                       + ProjectStructure.getProjectFileExtension());

            tempProjectFile.renameTo(newProjectFile);



            GeneralUtils.copyDirIntoDir(ProjectStructure.getMorphologiesDir(oldProjFile.getParentFile()),
                                        ProjectStructure.getMorphologiesDir(newProjdir), false, true);

        }
        catch (IOException ex)
        {
            GuiUtils.showErrorMessage(logger, "Problem creating copy of the project", ex, this);
            return;
        }
        try
        {
            File impMorphDir = ProjectStructure.getImportedMorphologiesDir(oldProjFile.getParentFile(), false);

            if (impMorphDir!=null)
            {
                GeneralUtils.copyDirIntoDir(impMorphDir,
                                            ProjectStructure.getImportedMorphologiesDir(newProjdir, true),
                    false, true);
            }
        }
        catch (IOException ex)
        {
            logger.logError("Problem copying DirForImportedMorphologies", ex);
            // continuing...
        }

        File cellMechDir = ProjectStructure.getCellMechanismDir(oldProjFile.getParentFile(), false);
        if (cellMechDir!=null)
        {
            try
            {
                GeneralUtils.copyDirIntoDir(cellMechDir,
                                            ProjectStructure.getCellMechanismDir(newProjdir), true, true);
            }
            catch (IOException ex)
            {
                logger.logError("Problem copying DirFormportedCellProcesses", ex);
                // continuing...
            }
        }

        File oldCellProcDir = ProjectStructure.getCellProcessesDir(oldProjFile.getParentFile(), false);
        if (oldCellProcDir!=null)
        {
            try
            {
                GeneralUtils.copyDirIntoDir(oldCellProcDir,
                                            ProjectStructure.getCellProcessesDir(newProjdir, true), true, true);

            }
            catch (IOException ex)
            {
                logger.logError("Problem copying DirFormportedCellProcesses", ex);
                // continuing...
            }
        }



        doLoadProject(newProjectFile.getAbsolutePath());

        projManager.getCurrentProject().setProjectName(newProjectName);

        ProjectStructure.getSimulationsDir(projManager.getCurrentProject().getProjectMainDirectory());

        projManager.getCurrentProject().markProjectAsEdited();
        doSave();


    }


    void jButtonSimPlotAdd_actionPerformed(ActionEvent e)
    {
        if (projManager.getCurrentProject().cellGroupsInfo.getNumberCellGroups()==0)
        {
            GuiUtils.showErrorMessage(logger, "Please add one or more Cell Groups before proceeding", null, this);
            return;
        }

        Vector allSimRefs = projManager.getCurrentProject().simPlotInfo.getAllSimPlotRefs();
        logger.logComment("All refs: "+ allSimRefs);
        int suggestedNum = 0;
        String suggestedRef = "Var_"+ suggestedNum;

        while (allSimRefs.contains(suggestedRef))
        {
            suggestedNum++;
            suggestedRef = "Var_"+ suggestedNum;
        }


        SimPlotDialog dlg
            = new SimPlotDialog(this,suggestedRef,
                                    projManager.getCurrentProject());

        Dimension dlgSize = dlg.getPreferredSize();
        Dimension frmSize = getSize();
        Point loc = getLocation();
        dlg.setLocation( (frmSize.width - dlgSize.width) / 2 + loc.x,
                        (frmSize.height - dlgSize.height) / 2 + loc.y);
        dlg.setModal(true);
        dlg.pack();
        dlg.setVisible(true);

        if (dlg.cancelled)
        {
            logger.logComment("They've changed their mind...");
            return;
        }
        projManager.getCurrentProject().markProjectAsEdited();
        projManager.getCurrentProject().simPlotInfo.addSimPlot(dlg.getFinalSimPlot());
        refreshTabInputOutput();


        if (this.projManager.getCurrentProject().simConfigInfo.getNumSimConfigs()==1)
        {
            projManager.getCurrentProject().simConfigInfo.getDefaultSimConfig().addPlot(dlg.getFinalSimPlot().getPlotReference());
            logger.logComment("Now plots in default SimConfig: "+ projManager.getCurrentProject().simConfigInfo.getDefaultSimConfig().getPlots());
        }
        else
        {
            GuiUtils.showInfoMessage(logger, "Added variable to plot/save", "There is more than one Simulation Configuration. To include this variable to plot/save in one of them, go to tab Generate.", this);
        }


    }


    void jButtonSimPlotDelete_actionPerformed(ActionEvent e)
    {
        int selectedRow = jTableSimPlot.getSelectedRow();

        if (selectedRow < 0)
        {
            logger.logComment("No row selected...");
            return;
        }
        projManager.getCurrentProject().simPlotInfo.deleteSimPlot(selectedRow);

        projManager.getCurrentProject().markProjectAsEdited();
        logger.logComment("Removed row: " + selectedRow);

        projManager.getCurrentProject().simConfigInfo.validateStoredSimConfigs(projManager.getCurrentProject());


        refreshTabInputOutput();

    }
    void jButtonSimPlotEdit_actionPerformed(ActionEvent e)
    {
        int selectedRow = jTableSimPlot.getSelectedRow();

        if (selectedRow < 0)
        {
            logger.logComment("No row selected...");
            return;
        }

        SimPlot selectedSimPlot = projManager.getCurrentProject().simPlotInfo.getSimPlot(selectedRow);

        SimPlotDialog dlg
            = new SimPlotDialog(this, selectedSimPlot.getPlotReference(),
                                projManager.getCurrentProject());

        dlg.setSimPlot(selectedSimPlot);

        Dimension dlgSize = dlg.getPreferredSize();
        Dimension frmSize = getSize();
        Point loc = getLocation();
        dlg.setLocation( (frmSize.width - dlgSize.width) / 2 + loc.x,
                        (frmSize.height - dlgSize.height) / 2 + loc.y);
        dlg.setModal(true);
        dlg.pack();
        dlg.setVisible(true);

        if (dlg.cancelled)
        {
            logger.logComment("They've changed their mind...");
            return;
        }
        projManager.getCurrentProject().markProjectAsEdited();
        projManager.getCurrentProject().simPlotInfo.updateSimPlot(dlg.getFinalSimPlot());
        refreshTabInputOutput();


    }

    void jButtonSimStimAdd_actionPerformed(ActionEvent e)
    {
        if (projManager.getCurrentProject().cellGroupsInfo.getNumberCellGroups()==0)
        {
            GuiUtils.showErrorMessage(logger, "Please add one or more Cell Groups before proceeding", null, this);
            return;
        }
        Vector allStimRefs = projManager.getCurrentProject().elecInputInfo.getAllStimRefs();
        logger.logComment("All refs: "+ allStimRefs);
        int suggestedNum = 0;
        String suggestedRef = "Input_"+ suggestedNum;

        while (allStimRefs.contains(suggestedRef))
        {
            suggestedNum++;
            suggestedRef = "Input_"+ suggestedNum;
        }


        StimDialog dlg
            = new StimDialog(this,suggestedRef,
                                    projManager.getCurrentProject());

        Dimension dlgSize = dlg.getPreferredSize();
        Dimension frmSize = getSize();
        Point loc = getLocation();
        dlg.setLocation( (frmSize.width - dlgSize.width) / 2 + loc.x,
                        (frmSize.height - dlgSize.height) / 2 + loc.y);
        dlg.setModal(true);
        dlg.pack();
        dlg.setVisible(true);

        if (dlg.cancelled)
        {
            logger.logComment("They've changed their mind...");
            return;
        }
        projManager.getCurrentProject().markProjectAsEdited();
        projManager.getCurrentProject().elecInputInfo.addStim(dlg.getFinalStim());
        refreshTabInputOutput();


        if (this.projManager.getCurrentProject().simConfigInfo.getNumSimConfigs()==1)
        {
            projManager.getCurrentProject().simConfigInfo.getDefaultSimConfig().addInput(dlg.getFinalStim().getReference());
            logger.logComment("Now inputs in default SimConfig: "+ projManager.getCurrentProject().simConfigInfo.getDefaultSimConfig().getInputs());
        }
        else
        {
            GuiUtils.showInfoMessage(logger, "Added Input", "There is more than one Simulation Configuration. To include this Input in one of them, go to tab Generate.", this);
        }


    }



    void jButtonSimStimDelete_actionPerformed(ActionEvent e)
    {
        int selectedRow = jTableStims.getSelectedRow();

        if (selectedRow < 0)
        {
            logger.logComment("No row selected...");
            return;
        }
        projManager.getCurrentProject().elecInputInfo.deleteStim(selectedRow);

        projManager.getCurrentProject().markProjectAsEdited();

        logger.logComment("Removed row: " + selectedRow);

        projManager.getCurrentProject().simConfigInfo.validateStoredSimConfigs(projManager.getCurrentProject());


        refreshTabInputOutput();

    }


    void jButtonSimStimEdit_actionPerformed(ActionEvent e)
    {
        int selectedRow = jTableStims.getSelectedRow();

        if (selectedRow < 0)
        {
            logger.logComment("No row selected...");
            return;
        }

        this.projManager.getCurrentProject().markProjectAsEdited();

        StimulationSettings selectedStim = projManager.getCurrentProject().elecInputInfo.getStim(selectedRow);

        StimDialog dlg
            = new StimDialog(this,selectedStim.getReference(),
                                    projManager.getCurrentProject());


        dlg.setStim(selectedStim);

        Dimension dlgSize = dlg.getPreferredSize();
        Dimension frmSize = getSize();
        Point loc = getLocation();
        dlg.setLocation( (frmSize.width - dlgSize.width) / 2 + loc.x,
                        (frmSize.height - dlgSize.height) / 2 + loc.y);
        dlg.setModal(true);
        dlg.pack();
        dlg.setVisible(true);

        if (dlg.cancelled)
        {
            logger.logComment("They've changed their mind...");
            return;
        }
        projManager.getCurrentProject().markProjectAsEdited();
        projManager.getCurrentProject().elecInputInfo.updateStim(dlg.getFinalStim());
        refreshTabInputOutput();

    }

    void jMenuItemHelp_actionPerformed(ActionEvent e)
    {

        logger.logComment("Going to show help menu...");

        File f = new File(ProjectStructure.getMainHelpFile());
        try
        {
            HelpFrame simpleViewer = HelpFrame.showFrame(f.toURL(), f.getAbsolutePath(), false);

            //System.out.println("Created viewer");

            simpleViewer.setFrameSize(800, 600);

            Dimension screenSize = Toolkit.getDefaultToolkit().getScreenSize();
            Dimension frameSize = simpleViewer.getSize();

            if (frameSize.height > screenSize.height)
                frameSize.height = screenSize.height;
            if (frameSize.width > screenSize.width)
                frameSize.width = screenSize.width;

            simpleViewer.setLocation( (screenSize.width - frameSize.width) / 2,
                                     (screenSize.height - frameSize.height) / 2);

            simpleViewer.setVisible(true);
        }
        catch (IOException io)
        {
            GuiUtils.showErrorMessage(logger, "Problem showing help frame", io, this);
        }

    }


    public void jButtonNeuroMLValidate_actionPerformed(ActionEvent e)
    {
        logger.logComment("Validating...");

        File schemaFile = GeneralProperties.getNeuroMLSchemaFile();
        try
        {
            SchemaFactory factory = SchemaFactory.newInstance(XMLConstants.W3C_XML_SCHEMA_NS_URI);

            logger.logComment("Found the XSD file: " + schemaFile.getAbsolutePath());

            Source schemaFileSource = new StreamSource(schemaFile);
            Schema schema = factory.newSchema(schemaFileSource);

            Validator validator = schema.newValidator();

            String filename = jComboBoxNeuroML.getSelectedItem().toString();
            filename = filename.substring(0,filename.indexOf("(")).trim();
            Source xmlFileSource = new StreamSource(new File(filename));

            validator.validate(xmlFileSource);

        }
        catch (Exception ex)
        {
            GuiUtils.showErrorMessage(logger, "Problem validating the NeuroML file. Note that the file was validated\n"
                                      + "against the version of the NeuroML schema ("+GeneralProperties.getNeuroMLVersionNumber()+") included with this distribution\n"
                                      + "of neuroConstruct. To validate it against current and past schema see:\n"
                                      + GeneralProperties.getWebsiteNMLValidator(), ex, this);

            return;
        }

        GuiUtils.showInfoMessage(logger, "Success", "NeuroML file is well formed and valid, according to schema:\n"
                                 + schemaFile.getAbsolutePath(), this);

    }


    void jButtonCellTypeEditDesc_actionPerformed(ActionEvent e)
    {
        //int selIndex = jComboBoxCellTypes.getSelectedIndex();
        Cell cell = (Cell)jComboBoxCellTypes.getSelectedItem();

        String oldDecs = new String(cell.getCellDescription());

        SimpleTextInput sti = SimpleTextInput.showString(cell.getCellDescription(),
                                                         "Description of Cell: " + cell.getInstanceName(),
                                                         12,
                                                         false,
                                                         false,
                                                         .4f,
                                                         this);

        String newDesc = sti.getString();
        cell.setCellDescription(newDesc);


        if (!oldDecs.equals(newDesc))
        {
            projManager.getCurrentProject().markProjectAsEdited();
            this.refreshTabCellTypes();
        }


    }

    void jButtonCellTypeBioPhys_actionPerformed(ActionEvent e)
    {



        Cell cell = (Cell)jComboBoxCellTypes.getSelectedItem();
        NumberGenerator ngInitPot = NumberGeneratorDialog.showDialog(this,"Initial Membrane Potential",
                                                                     "Initial Membrane Potential, units: "+
                                                                     UnitConverter.voltageUnits[UnitConverter.NEUROCONSTRUCT_UNITS].getSymbol(),
                                                                     cell.getInitialPotential());

        cell.setInitialPotential(ngInitPot);
/*
        NumberGenerator ngSpecAxRes = NumberGeneratorDialog.showDialog(this,
                                                                       "Specific Axial Resistance, units: "+
                                                                     UnitConverter.specificAxialResistanceUnits[UnitConverter.NEUROCONSTRUCT_UNITS].getSymbol(),
                                                                     cell.getSpecAxRes());
      cell.setSpecAxRes(ngSpecAxRes);

      NumberGenerator ngSpecCap = NumberGeneratorDialog.showDialog(this,
                                                                   "Specific Capacitance, units: "+
                                                                     UnitConverter.specificCapacitanceUnits[UnitConverter.NEUROCONSTRUCT_UNITS].getSymbol(),
                                                                   cell.getSpecCapacitance());
      cell.setSpecCapacitance(ngSpecCap);

*/
       projManager.getCurrentProject().markProjectAsEdited();

        refreshTabCellTypes();
    }

    void jButtonCellTypeOtherProject_actionPerformed(ActionEvent e)
    {
        // set to parent of project dir...
        File defaultDir = projManager.getCurrentProject().getProjectFile().getParentFile().getParentFile();


        Frame frame = (Frame)this;
        JFileChooser chooser = new JFileChooser();
        chooser.setDialogType(JFileChooser.OPEN_DIALOG);
        chooser.setDialogTitle("Choose neuroConstruct project from which to import Cell Type");

        try
        {
            chooser.setCurrentDirectory(defaultDir);
            logger.logComment("Set Dialog dir to: " + defaultDir);
        }
        catch (Exception ex)
        {
            logger.logError("Problem with default dir setting: " + defaultDir, ex);
        }
        SimpleFileFilter fileFilter
            = new SimpleFileFilter(new String[]
                                   {ProjectStructure.getProjectFileExtension()}
                                   ,
                                   "neuroConstruct files. Extension: *" + ProjectStructure.getProjectFileExtension());

        chooser.setFileFilter(fileFilter);

        //chooser.sett

        int retval = chooser.showDialog(frame, null);

        if (retval == JFileChooser.APPROVE_OPTION)
        {
            try
            {
                logger.logComment(">>>>  Loading project: " + chooser.getSelectedFile());

                Project otherProj = Project.loadProject(chooser.getSelectedFile(), this);

                logger.logComment("<<<<  Loaded project: " + otherProj.getProjectFileName());

                ArrayList<String> otherCellTypes = otherProj.cellManager.getAllCellTypeNames();

                if (otherCellTypes.size()==0)
                {
                    GuiUtils.showErrorMessage(logger, "No Cell Types found in that project.", null, this);
                    return;
                }

                Object selection = JOptionPane.showInputDialog(this,
                            "Please select the Cell Type to import from project "+otherProj.getProjectName(),
                            "Select Cell Type",
                            JOptionPane.QUESTION_MESSAGE,
                            null,
                            otherCellTypes.toArray(),
                            otherCellTypes.get(0));

                if (selection==null)
                {
                    logger.logComment("No selection made...");
                    return;
                }
                logger.logComment("Selection: "+ selection);

                Cell importedCell = otherProj.cellManager.getCell((String)selection);

                String originalCellTypeName = importedCell.getInstanceName();

                if (projManager.getCurrentProject().cellManager.getCell((String)selection)!=null)
                {
                    String suggestedName = importedCell.getInstanceName()+"_"+otherProj.getProjectName();
                    String newName = JOptionPane.showInputDialog(this, "This project already contains a Cell Type "+ importedCell.getInstanceName()
                                                                 +". Please enter\nanother name for the Cell Type", suggestedName);

                    if (newName==null)
                    {
                        logger.logComment("User cancelled...");
                        return;
                    }



                    importedCell.setInstanceName(newName);
                }
                projManager.getCurrentProject().cellManager.addCellType(importedCell);



                jComboBoxCellTypes.setSelectedItem(importedCell);

                ArrayList cellMechs = importedCell.getAllChannelMechanisms(true);
                
                ArrayList<String> synapses = importedCell.getAllAllowedSynapseTypes();

                cellMechs.addAll(synapses);

                Vector allStims = otherProj.elecInputInfo.getAllStims();
                for (int i = 0; i < allStims.size(); i++)
                {
                    StimulationSettings next = (StimulationSettings)allStims.elementAt(i);
                    logger.logComment("Investigating stim on other proj: "+ next);
                    //if ()
                    String cellType = otherProj.cellGroupsInfo.getCellType(next.getCellGroup());

                    if (cellType != null &&
                        cellType.equals(originalCellTypeName) &&
                        next instanceof RandomSpikeTrainSettings)
                    {
                        String spikeCPName = ((RandomSpikeTrainSettings)next).getSynapseType();
                        cellMechs.add(spikeCPName);
                    }
                }

                logger.logComment("Cell mechs on imported cell: "+ cellMechs);

                for (int i = 0; i < cellMechs.size(); i++)
                {
                    String cellMechName = null;
                    if (cellMechs.get(i) instanceof ChannelMechanism)
                    {
                        ChannelMechanism nextChanMech = (ChannelMechanism)cellMechs.get(i);
                        cellMechName = nextChanMech.getName();
                    }
                    else
                    {
                        cellMechName = (String)cellMechs.get(i);
                    }


                    CellMechanism importedCellProc = otherProj.cellMechanismInfo.getCellMechanism(cellMechName);

                    logger.logComment("---  Imported cell mech: ");
                    //importedCellProc.printDetails();

                    if (projManager.getCurrentProject().cellMechanismInfo.getAllCellMechanismNames().contains(cellMechName))
                    {
                        String oldName = new String(cellMechName);


                        int useExisting = JOptionPane.showConfirmDialog(this, "This project already contains a Cell Process called "
                                                          + importedCellProc.getInstanceName()
                                                          +". Do you want to use the current project's "
                                                          +importedCellProc.getInstanceName()+" on the Cell:\n"
                                                          + importedCell.getInstanceName()
                                                          +"?\n\nSelect No to import and rename the Cell Process as used in project: "
                                                          + otherProj.getProjectName() + "?",
                                                          "Use current Cell Process?",
                                                          JOptionPane.YES_NO_CANCEL_OPTION);

                        if (useExisting==JOptionPane.CANCEL_OPTION)
                        {
                            logger.logComment("User cancelled...");
                            return;
                        }
                        else if (useExisting==JOptionPane.NO_OPTION)
                        {
                            String suggestedName = importedCellProc.getInstanceName()+"_"+otherProj.getProjectName();
                            String newName = JOptionPane.showInputDialog(this,
                                "Please enter another name for the Cell Process which is present on the Cell: "
                                + importedCell.getInstanceName(), suggestedName);

                            if (newName==null)
                            {
                                logger.logComment("User cancelled...");
                                return;
                            }


                            importedCellProc.setInstanceName(newName);

                            Hashtable chanMechVsGroups = importedCell.getChanMechsVsGroups();
                            Enumeration enumeration = chanMechVsGroups.keys();
                            while (enumeration.hasMoreElements())
                            {
                                ChannelMechanism next = (ChannelMechanism) enumeration.nextElement();
                                if (next.getName().equals(oldName)) next.setName(newName);
                            }

                            Hashtable synapsesVsGroups = importedCell.getSynapsesVsGroups();
                            enumeration = synapsesVsGroups.keys();
                            while (enumeration.hasMoreElements())
                            {
                                String next = (String) enumeration.nextElement();
                                if (next.equals(oldName))
                                {
                                    synapsesVsGroups.put(newName, synapsesVsGroups.get(oldName));
                                    synapsesVsGroups.remove(oldName);
                                }
                            }

                            if (importedCellProc instanceof FileBasedMembraneMechanism)
                            {
                                logger.logComment("Copying the impl file into the project");
                                FileBasedMembraneMechanism fbmp = (FileBasedMembraneMechanism) importedCellProc;
                                MechanismImplementation procImpl[] = fbmp.getMechanismImpls();
                                for (int j = 0; j < procImpl.length; j++)
                                {
                                    File nextFile = procImpl[j].getImplementingFileObject(otherProj, fbmp.getInstanceName());
                                    //File newLocation = ProjectStructure.getFileBasedCellProcessesDir(projManager.getCurrentProject().getProjectMainDirectory(), true);##
                                    File newLocation = ProjectStructure.getDirForCellMechFiles(otherProj, fbmp.getInstanceName());

                                    File newFile = null;
                                    try
                                    {
                                        newFile = GeneralUtils.copyFileIntoDir(nextFile,
                                            newLocation);
                                        ((FileBasedMembraneMechanism)importedCellProc).getMechanismImpls()[j].setImplementingFile(newFile.getName());
                                    }
                                    catch (IOException ex)
                                    {
                                        GuiUtils.showErrorMessage(logger, "Problem when including new Cell Process", ex, this);
                                        return;
                                    }
                                }
                            }
                            else if (importedCellProc instanceof ChannelMLCellMechanism)
                            {
                                logger.logComment("Copying the ChannelMLCellProcess files into the project");
                                //ChannelMLCellMechanism cmlp = (ChannelMLCellMechanism) importedCellProc;

                                File otherProjCellProcFilesLoc = new File(
                                    ProjectStructure.getCellMechanismDir(otherProj.getProjectMainDirectory()),
                                    oldName);

                                File thisProjCellProcFilesLoc = new File(
                                    ProjectStructure.getCellMechanismDir(projManager.getCurrentProject().getProjectMainDirectory()),
                                    newName);

                                thisProjCellProcFilesLoc.mkdir();

                                GeneralUtils.copyDirIntoDir(otherProjCellProcFilesLoc, thisProjCellProcFilesLoc, false, true);




                                /*
                                ProcessImplementation procImpl[] = fbmp.getProcessImpls();
                                for (int j = 0; j < procImpl.length; j++)
                                {
                                    File nextFile = procImpl[j].getImplementingFileObject(otherProj);
                                    File newLocation = .getFileBasedCellProcessesDir(projManager.getCurrentProject().getProjectMainDirectory());
                                    File newFile = null;
                                    try
                                    {
                                        newFile = GeneralUtils.copyFileIntoDir(nextFile,
                                            newLocation);
                                        ((FileBasedMembraneProcess)importedCellProc).getProcessImpls()[j].setImplementingFile(newFile.getName());
                                    }
                                    catch (IOException ex)
                                    {
                                        GuiUtils.showErrorMessage(logger, "Problem when including new Cell Process", ex, this);
                                        return;
                                    }
                                }*/
                            }



                            projManager.getCurrentProject().cellMechanismInfo.addCellMechanism(importedCellProc);


                        }
                        else
                        {
                            logger.logComment("Using existing Cell Process...");
                        }


                    }
                    else
                    {
                        logger.logComment("Cell process: "+ importedCellProc.getInstanceName()+ " not already in proj...");
                        projManager.getCurrentProject().cellMechanismInfo.addCellMechanism(importedCellProc);

                        //String oldName = new String(cellProcName);


                        if (importedCellProc instanceof FileBasedMembraneMechanism)
                        {

                            FileBasedMembraneMechanism fbmp = (FileBasedMembraneMechanism) importedCellProc;

                            logger.logComment("Copying the impl files from: "+fbmp.getInstanceName()+" into the project");

                            MechanismImplementation procImpl[] = fbmp.getMechanismImpls();

                            for (int j = 0; j < procImpl.length; j++)
                            {
                                logger.logComment("Looking at sim env: "+ procImpl[j].getSimulationEnvironment()
                                                  + ", file: "+ procImpl[j].getImplementingFile());

                                File nextFile = procImpl[j].getImplementingFileObject(otherProj, fbmp.getInstanceName());

                                File newLocation = ProjectStructure.getFileBasedCellProcessesDir(projManager.getCurrentProject().getProjectMainDirectory(), true);

                                File newFile = null;
                                try
                                {
                                    newFile = GeneralUtils.copyFileIntoDir(nextFile,
                                                                           newLocation);

                                    ((FileBasedMembraneMechanism)importedCellProc).getMechanismImpls()[j].setImplementingFile(newFile.getName());


                                }
                                catch (IOException ex)
                                {
                                    GuiUtils.showErrorMessage(logger, "Problem when including new Cell Process", ex, this);
                                    return;
                                }

                            }
                        }
                        else if (importedCellProc instanceof ChannelMLCellMechanism)
                        {
                            logger.logComment("Copying the ChannelMLCellProcess files into the project");
                            //ChannelMLCellMechanism cmlp = (ChannelMLCellMechanism) importedCellProc;

                            File otherProjCellProcFilesLoc = new File(
                                ProjectStructure.getCellMechanismDir(otherProj.getProjectMainDirectory()),
                                cellMechName);

                            File thisProjCellProcFilesLoc = new File(
                                ProjectStructure.getCellMechanismDir(projManager.getCurrentProject().
                                                                     getProjectMainDirectory()),
                                cellMechName);

                            thisProjCellProcFilesLoc.mkdir();

                            GeneralUtils.copyDirIntoDir(otherProjCellProcFilesLoc, thisProjCellProcFilesLoc, false, true);
                        }

                    }



                }
                float thisTemp = projManager.getCurrentProject().simulationParameters.getTemperature();
                float otherTemp = otherProj.simulationParameters.getTemperature();

                if (thisTemp!=otherTemp)
                {
                    GuiUtils.showInfoMessage(logger, "Warning",
                                             "Please note that the imported cell's project simulation temperature was "+otherTemp
                                             +" whereas this project is set to run simulations at "+thisTemp+".\n This will lead to different behaviours of the cell"
                                             + " if there are channels with temperature dependent rate equations.", this);
                }

                projManager.getCurrentProject().markProjectAsEdited();
                this.refreshAll();

            }
            catch (Exception ex2)
            {
                GuiUtils.showErrorMessage(logger, "Problem adding the Cell", ex2, this);

                return;
            }
        }

    }

    void jMenuItemJava_actionPerformed(ActionEvent e)
    {
        Properties props = System.getProperties();
        Enumeration names = props.propertyNames();

        int idealPropNameWidth = 30;
        int idealTotalWidth = 120;

        StringBuffer sb= new StringBuffer();

        sb.append("Java system properties:\n\n");

        while (names.hasMoreElements())
        {
            String propName = (String) names.nextElement();
            String val = props.getProperty(propName);
            propName = propName+": ";
            if (propName.length()<=idealPropNameWidth)
            {

                for (int i = propName.length(); i <= idealPropNameWidth ; i++)
                {
                        propName = propName + " ";
                }
            }
            sb.append(GeneralUtils.wrapLine(propName + val, "\n", idealTotalWidth) + "\n");
        }


        boolean useHtml = false;

        SimpleViewer.showString(sb.toString(), "Java system properties", 11, false, useHtml);

    }

    void jMenuItemGenNetwork_actionPerformed(ActionEvent e)
    {
        if (!projManager.projectLoaded()) return;

        jTabbedPaneMain.setSelectedIndex(jTabbedPaneMain.indexOfTab(this.GENERATE_TAB));

        doGenerate();

    }


    void jMenuItemGenNeuron_actionPerformed(ActionEvent e)
    {
        if (!projManager.projectLoaded()) return;

        jTabbedPaneMain.setSelectedIndex(jTabbedPaneMain.indexOfTab(this.EXPORT_TAB));
        jTabbedPaneExportFormats.setSelectedIndex(jTabbedPaneExportFormats.indexOfTab(this.NEURON_SIMULATOR_TAB));
        jTabbedPaneNeuron.setSelectedIndex(jTabbedPaneNeuron.indexOfTab(this.NEURON_TAB_GENERATE));

        doCreateHoc(NeuronFileManager.RUN_LOCALLY);

    }



    void jMenuItemGenGenesis_actionPerformed(ActionEvent e)
    {
        if (!projManager.projectLoaded()) return;

        jTabbedPaneMain.setSelectedIndex(jTabbedPaneMain.indexOfTab(this.EXPORT_TAB));
        jTabbedPaneExportFormats.setSelectedIndex(jTabbedPaneExportFormats.indexOfTab(this.GENESIS_SIMULATOR_TAB));
        jTabbedPaneGenesis.setSelectedIndex(jTabbedPaneGenesis.indexOfTab(this.GENESIS_TAB_GENERATE));

        doCreateGenesis();

    }


    void jMenuItemPrevSims_actionPerformed(ActionEvent e)
    {
        if (!projManager.projectLoaded()) return;

        jTabbedPaneMain.setSelectedIndex(jTabbedPaneMain.indexOfTab(this.VISUALISATION_TAB));

        doViewPrevSimulations();
    }


    void jMenuItemDataSets_actionPerformed(ActionEvent e)
    {
        if (!projManager.projectLoaded()) return;

        jTabbedPaneMain.setSelectedIndex(jTabbedPaneMain.indexOfTab(this.VISUALISATION_TAB));

        doShowDataSets();

    }

    void doShowDataSets()
    {
        //File dataSetDir = ProjectStructure.getDataSetsDir(projManager.getCurrentProject().getProjectMainDirectory());

        DataSetManager frame = new DataSetManager(projManager.getCurrentProject(), false);

        Dimension dlgSize = frame.getPreferredSize();
        Dimension frmSize = getSize();
        Point loc = getLocation();
        frame.setLocation( (frmSize.width - dlgSize.width) / 2 + loc.x,
                        (frmSize.height - dlgSize.height) / 2 + loc.y);


        frame.pack();
        frame.setVisible(true);

    }




}
