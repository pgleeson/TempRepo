/**
 * neuroConstruct
 *
 * Software for developing large scale 3D networks of biologically realistic neurons
 * Copyright (c) 2008 Padraig Gleeson
 * UCL Department of Physiology
 *
 * Development of this software was made possible with funding from the
 * Medical Research Council
 *
 */

package ucl.physiol.neuroconstruct.neuron;

import java.io.*;
import java.util.*;

import javax.vecmath.*;

import ucl.physiol.neuroconstruct.cell.*;
import ucl.physiol.neuroconstruct.cell.utils.*;
import ucl.physiol.neuroconstruct.gui.*;
import ucl.physiol.neuroconstruct.mechanisms.*;
import ucl.physiol.neuroconstruct.neuroml.*;
import ucl.physiol.neuroconstruct.project.*;
import ucl.physiol.neuroconstruct.project.packing.*;
import ucl.physiol.neuroconstruct.project.stimulation.*;
import ucl.physiol.neuroconstruct.simulation.*;
import ucl.physiol.neuroconstruct.utils.*;
import ucl.physiol.neuroconstruct.utils.units.*;
import ucl.physiol.neuroconstruct.utils.python.*;
import ucl.physiol.neuroconstruct.project.GeneratedPlotSaves.*;
import ucl.physiol.neuroconstruct.hpc.mpi.*;
import ucl.physiol.neuroconstruct.neuroml.hdf5.*;
import ucl.physiol.neuroconstruct.project.GeneratedNetworkConnections.*;

/**
 * Main file for generating the script files for NEURON
 *
 * @author Padraig Gleeson
 *  
 */

public class NeuronFileManager
{
    private static ClassLogger logger = new ClassLogger("NeuronFileManager");

    /**
     * Various options for running the generated code: Generate hoc
     */
    public static final int RUN_HOC = 0;
    
    /**
     * Various options for running the generated code: Generate condor code (semi deprecated)
     */
    public static final int RUN_VIA_CONDOR = 1;
    
    /**
     * Various options for running the generated code: Generate hoc/Python
     */
    public static final int RUN_PYTHON_XML = 3;
    
    /**
     * Various options for running the generated code: Generate hoc/Python
     */
    public static final int RUN_PYTHON_HDF5 = 4;

    /**
     * The random seed placed into the generated NEURON code
     */
    private long randomSeed = 0;

    /**
     * The runMode used in the generated NEURON code
     */
    private int genRunMode = -1;
    
    /**
     * The time last taken to generate the main files
     */
    private float genTime = -1;

    
    private Project project = null;

    private File mainHocFile = null;
    
    private File mainPythonFile = null;
    
    private File runPythonFile = null;

    private boolean hocFileGenerated = false;

    /**
     * A list of the *.mod files which will be needed by the cells in the simulation
     */
    private Vector<String> cellMechFilesGenAndIncl = new Vector<String>();

    /**
     * A list of the *.mod files which will be needed by the cells in the simulation
     */
    private Vector<String> stimModFilesRequired = new Vector<String>();

    /**
     * A list of the NEURON template files which will be needed by the cells in the simulation
     */
    private Vector<String> cellTemplatesGenAndIncluded = new Vector<String>();
    
    /**
     * To manage multiple runs, as specified through the GUI. Will prob be removed in favour of easier 
     * Python script based project opening/generation/simulation running.
     */
    private MultiRunManager multiRunManager = null;
    
    
    private Hashtable<String, Integer> nextColour = new Hashtable<String, Integer>();

    private Vector<String> graphsCreated = new Vector<String>();

    private static boolean addComments = true;

    private SimConfig simConfig = null;


    File utilsFile = new File(ProjectStructure.getNeuronUtilsFile());
    
    File cellCheckFile = new File(ProjectStructure.getNeuronCellCheckFile());
    
    
    public static final String EXT_CURR_CLAMP_MOD = "CurrentClampExt.mod";
    
    public static final String FORCE_REGENERATE_MODS_FILENAME = "regenerateMods";
    
    /*
     * Will recompile mods at least once  
     */
    private boolean firstRecompileComplete = false;
    
    
    private boolean quitAfterRun = false;
            
    private NeuronFileManager()
    {

    }

    public NeuronFileManager(Project project)
    {
        this.project = project;
        addComments = project.neuronSettings.isGenerateComments();

    }

    public static boolean addComments()
    {
        return addComments;
    }

    public void reset()
    {
        cellTemplatesGenAndIncluded = new Vector<String>();
        cellMechFilesGenAndIncl = new Vector<String>();
        stimModFilesRequired =  new Vector<String>();
        
        graphsCreated = new Vector<String>();
        
        nextColour = new Hashtable<String, Integer>(); // reset it...
        
        addComments = project.neuronSettings.isGenerateComments();
        
        genRunMode = -1;
        genTime = -1;

    }
    

    public void generateTheNeuronFiles(SimConfig simConfig,
                                       MultiRunManager multiRunManager,
                                       int runMode,
                                       long randomSeed) throws NeuronException, IOException
    {
        logger.logComment("****  Starting generation of the hoc files...  ****");
        
        reset();

        long generationTimeStart = System.currentTimeMillis();
        
        this.simConfig = simConfig;
        
        this.genRunMode = runMode;

        this.multiRunManager = multiRunManager;

        this.removeAllPreviousFiles();

        // Reinitialise the neuroConstruct rand num gen with the neuroConstruct seed
        ProjectManager.reinitialiseRandomGenerator();

        this.randomSeed = randomSeed;

        FileWriter hocWriter = null;
        FileWriter pythonWriter = null;
        FileWriter pythonRunWriter = null;
 
        try
        {
            File dirForNeuronFiles = ProjectStructure.getNeuronCodeDir(project.getProjectMainDirectory());
            
            if (isRunModePythonBased(runMode))
            {
                File pyNmlUtils = ProjectStructure.getPythonNeuroMLUtilsDir(project.getProjectMainDirectory());

                File pyNeuUtils = ProjectStructure.getPythonNeuronUtilsDir(project.getProjectMainDirectory());
                
                File toDir1 = new File(dirForNeuronFiles, pyNmlUtils.getName());

                GeneralUtils.copyDirIntoDir(pyNmlUtils, toDir1, true, true);
                
                File toDir2 = new File(dirForNeuronFiles, pyNeuUtils.getName());
                GeneralUtils.copyDirIntoDir(pyNeuUtils, toDir2, true, true);
                
                
                if (runMode== RUN_PYTHON_XML)
                {
                    File networkFile = new File(dirForNeuronFiles, NetworkMLConstants.DEFAULT_NETWORKML_FILENAME_XML);

                    try
                    {

                        project.saveNetworkStructureXML(networkFile,
                                                     false,
                                                     false,
                                                     simConfig.getName());
                    }
                    catch (NeuroMLException ex1)
                    {
                        GuiUtils.showErrorMessage(logger, "Problem saving network in NeuroML XML file: "+networkFile , ex1, null);
                    }
                } 
                else if (runMode == RUN_PYTHON_HDF5)
                {
                    
                    File networkFile = new File(dirForNeuronFiles, NetworkMLConstants.DEFAULT_NETWORKML_FILENAME_HDF5);
                    try
                    {
                        NetworkMLWriter.createNetworkMLH5file(networkFile, project);
                    }
                    catch (Hdf5Exception ex1)
                    {
                        GuiUtils.showErrorMessage(logger, "Problem saving network in NeuroML HDF5 file: "+ networkFile, ex1, null);
                    }
                }
                    


                mainPythonFile = new File(dirForNeuronFiles, project.getProjectName() + ".py");
                runPythonFile = new File(dirForNeuronFiles, "run_"+project.getProjectName() + ".py");
                
                pythonWriter = new FileWriter(mainPythonFile);
                pythonRunWriter = new FileWriter(runPythonFile);
                
                
                pythonRunWriter.write(PythonUtils.getFileHeader());
                pythonRunWriter.write(generatePythonRunFile());
                
                
                
                pythonWriter.write(PythonUtils.getFileHeader());
                
                pythonWriter.write(generatePythonIncludes());
                
                StringBuffer mainScript = new StringBuffer();
                
                //mainScript.append(generateWelcomeComments());
                
                mainScript.append(initialPythonSetup());

                mainScript.append(initialisePythonLogging());

                mainScript.append(generateCellGroups());
                
                mainScript.append(loadNetworkMLStructure());
                
                
                pythonWriter.write(PythonUtils.addMethodDef("loadNetwork", "",
                        mainScript.toString(), "This is the main function which will be called by the hoc file.\nSubject to change..."));


            }

            mainHocFile = new File(dirForNeuronFiles, project.getProjectName() + ".hoc");

            hocWriter = new FileWriter(mainHocFile);

            hocWriter.write(getHocFileHeader());


            //if (!simConfig.getMpiConf().isParallel())
                
            hocWriter.write(generateGUIInclude());
            
            hocWriter.write(generateWelcomeComments());

            hocWriter.write(generateHocIncludes());

            hocWriter.write(getHostname());

            hocWriter.write(initialiseParallel());
            
            hocWriter.write(generateRandomise());

            hocWriter.write(generateNeuronCodeBlock(NativeCodeLocation.BEFORE_CELL_CREATION));
            
           
            hocWriter.write(associateCellsWithNodes());
            
            
            if (isRunModePythonBased(runMode))
            {
                hocWriter.write(getHocPythonStartup(project));
                
                hocWriter.write(generateInitialParameters());
            }
            
            if (!isRunModePythonBased(runMode))
            {

                hocWriter.write(generateCellGroups());

                hocWriter.write(generateInitialParameters());
                hocWriter.flush();
    
                hocWriter.write(generateNetworkConnections());
                hocWriter.flush();
            }

            hocWriter.write(generateStimulations());

            hocWriter.write(generateAccess());
            
            if (runMode != RUN_VIA_CONDOR && !simConfig.getMpiConf().isParallel()) // No gui if it's condor or parallel...
            {
                if (project.neuronSettings.isGraphicsMode())
                {
                    hocWriter.write(generatePlots());

                    if (project.neuronSettings.isShowShapePlot())
                    {
                        hocWriter.write(generateShapePlot());
                    }

                }

            }
            
            hocWriter.write(generateInitHandlers());
            
            hocWriter.write(generateRunSettings());
            
            hocWriter.write(generateNeuronSimulationRecording());
                
            
            // Finishing up...
            
            if (!simConfig.getMpiConf().isParallel())
                hocWriter.write(generateGUIForRerunning());
           
            hocWriter.write(generateNeuronCodeBlock(NativeCodeLocation.AFTER_SIMULATION));
            
            if (simConfig.getMpiConf().isParallel())
                hocWriter.write(finishParallel());
            
            if (runMode == RUN_VIA_CONDOR || quitAfterRun)
                hocWriter.write(generateQuit());
            

            hocWriter.flush();
            hocWriter.close();
            
            if (isRunModePythonBased(runMode))
            {
                pythonWriter.flush();
                pythonWriter.close();
                
                pythonRunWriter.flush();
                pythonRunWriter.close();
            }

            if (utilsFile.getAbsoluteFile().exists())
            {
                GeneralUtils.copyFileIntoDir(utilsFile.getAbsoluteFile(), dirForNeuronFiles);
            }
            else
            {
                logger.logComment("File doesn't exist: "+ utilsFile.getAbsolutePath());
            }
            
            if (cellCheckFile.getAbsoluteFile().exists())
            {
                GeneralUtils.copyFileIntoDir(cellCheckFile.getAbsoluteFile(), dirForNeuronFiles);
            }
            else
            {
                logger.logComment("File doesn't exist: "+ utilsFile.getAbsolutePath());
            }

        }
        catch (IOException ex)
        {

            try
            {
                hocWriter.close();
            }
            catch (IOException ex1)
            {
            }
            catch (NullPointerException ex1)
            {
            }

            throw new NeuronException("Error writing to file: " + mainHocFile
                                      + "\n" + ex.getMessage()
                                      +
                "\nEnsure the NEURON files you are trying to generate are not currently being used");
        }
        //generatedRunMode = runMode;
        this.hocFileGenerated = true;
        

        long generationTimeEnd = System.currentTimeMillis();
        genTime = (float) (generationTimeEnd - generationTimeStart) / 1000f;

        logger.logComment("****  Created Main hoc file: " + mainHocFile+" in "+genTime+" seconds. **** \n");
        
        return;

    }
    
    private static String getHocPythonStartup(Project project)
    {
        StringBuffer response = new StringBuffer();    

        addMajorHocComment(response,"Setting up Python to allow loading in of NetworkML ");
        
        response.append("nrnpython(\"import sys\")\n");
        response.append("nrnpython(\"import os\")\n\n");
        
        addHocComment(response, "Adding current path to Python path");

        response.append("nrnpython(\"if sys.path.count(os.getcwd())==0: sys.path.append(os.getcwd())\")\n");
        

        response.append("nrnpython(\"import neuron\")\n");
        response.append("nrnpython(\"from neuron import hoc\")\n");
        response.append("nrnpython(\"import nrn\")\n\n");


        response.append("objref py\n");
        
        response.append("py = new PythonObject()\n\n");

        response.append("nrnpython(\"h = hoc.HocObject()\")\n\n");

        addHocComment(response, "Importing main Python file: "+project.getProjectName());
        
        response.append("nrnpython(\"import "+project.getProjectName()+"\")\n\n");
        response.append("nrnpython(\""+project.getProjectName()+".loadNetwork()\")\n\n");

        response.append("\n");
        return response.toString();
    }
    
    
    public void setQuitAfterRun(boolean quit)
    {
        this.quitAfterRun = quit;
    }
    


    /** @todo Put option on NEURON frame for this... */

    private void removeAllPreviousFiles()
    {
        cellTemplatesGenAndIncluded.removeAllElements();
        cellMechFilesGenAndIncl.removeAllElements();

        File hocFileDir = ProjectStructure.getNeuronCodeDir(project.getProjectMainDirectory());

        //GeneralUtils.removeAllFiles(hocFileDir, false, true, true);
        File[] allFiles = hocFileDir.listFiles();

        File modsDir = ProjectStructure.getNeuronCodeDir(project.getProjectMainDirectory());
        File forceRegenerateFile = new File(modsDir, NeuronFileManager.FORCE_REGENERATE_MODS_FILENAME);
        
        if (allFiles!=null)
        {
            for (int i = 0; i < allFiles.length; i++)
            {
                if (firstRecompileComplete &&
                    !forceRegenerateFile.exists() &&
                    !project.neuronSettings.isForceModFileRegeneration() && 
                    (allFiles[i].getName().endsWith(".mod") ||
                    allFiles[i].getName().endsWith(".dll") ||
                    allFiles[i].getName().equals(GeneralUtils.DIR_64BIT) ||
                    allFiles[i].getName().equals(GeneralUtils.DIR_I686) ||
                    allFiles[i].getName().equals(GeneralUtils.DIR_POWERPC)))
                {
                    logger.logComment("Leaving in place file: "+ allFiles[i]);
                }
                else
                {
                    if (allFiles[i].isDirectory())
                    {
                        GeneralUtils.removeAllFiles(allFiles[i], false, true, true);
                    }
                    else
                    {
                        allFiles[i].delete();
                    }
                }
            }
        }

    }

    public ArrayList<String> getGeneratedSimReferences()
    {
        if (multiRunManager== null)
            return new ArrayList<String>();
        return this.multiRunManager.getGeneratedSimReferences();
    }

    public Vector getGeneratedFilenames()
    {
        try
        {
            Vector<String> allFiles = new Vector<String>();
            for (int i = 0; i < cellTemplatesGenAndIncluded.size(); i++)
            {
                allFiles.add( (new File( cellTemplatesGenAndIncluded.get(i))).getName());
            }
            allFiles.add(getMainHocFile().getName());
            return allFiles;
        }
        catch (NeuronException e)
        {
            logger.logError("Files not yet generated!", e);
            return null;
        }
    }

    private static String getHocFileHeader()
    {
        StringBuffer response = new StringBuffer();
        response.append("//  ******************************************************\n");
        response.append("// \n");
        response.append("//     File generated by: neuroConstruct v"+GeneralProperties.getVersionNumber()+"\n");
        response.append("// \n");
        response.append("//  ******************************************************\n");

        response.append("\n");
        return response.toString();
    }

    private String generateGUIInclude()
    {
        StringBuffer response = new StringBuffer();
        response.append("load_file(\"nrngui.hoc\")" + "\n");
        addHocComment(response, "Initialising stopwatch for timing setup");
        response.append("startsw()\n\n");
        return response.toString();
    }

    private String generateNeuronCodeBlock(NativeCodeLocation ncl)
    {
        StringBuffer response = new StringBuffer();

        String text = project.neuronSettings.getNativeBlock(ncl);


        text = NativeCodeLocation.parseForSimConfigSpecifics(text, simConfig.getName());

        logger.logComment("Cleaned up to: "+ text);

        if (text == null || text.trim().length() == 0)
        {
            return "";
        }
        else
        {
            addHocComment(response, "Hoc commands to run at location: " + ncl.toString());
            response.append(text + "\n");
            addHocComment(response, "End of hoc commands to run at location: " + ncl.toString());

            return response.toString();
        }
    }

    private String generateInitHandlers()
    {
        StringBuffer response = new StringBuffer();

        NativeCodeLocation[] neuronFInitNcls = new NativeCodeLocation[]
            {
            NativeCodeLocation.BEFORE_INITIAL,
            NativeCodeLocation.AFTER_INITIAL,
            NativeCodeLocation.BEFORE_FINITIALIZE_RETURNS,
            NativeCodeLocation.START_FINITIALIZE};

        StringBuffer nativeBlocks = new StringBuffer();

        for (int i = 0; i < neuronFInitNcls.length; i++)
        {
            String text = project.neuronSettings.getNativeBlock(neuronFInitNcls[i]);

            text = NativeCodeLocation.parseForSimConfigSpecifics(text, simConfig.getName());
            logger.logComment("Cleaned up to: "+ text);


            if (text != null && text.trim().length() > 0)
            {
                int ref = neuronFInitNcls[i].getPositionReference();
                String objName = "fih_" + ref;
                String procName = "callfi" + ref;
                addHocComment(nativeBlocks, "Hoc commands to run at location: " + neuronFInitNcls[i].toString());
                nativeBlocks.append("objref " + objName + "\n");
                nativeBlocks.append(objName + " = new FInitializeHandler(" + neuronFInitNcls[i].getPositionReference() +
                                ", \"" +
                                procName + "()\")" + "\n");
                nativeBlocks.append("proc " + procName + "() {" + "\n");
                nativeBlocks.append(text + "\n");
                nativeBlocks.append("}" + "\n");

                addHocComment(nativeBlocks, "End of hoc commands to run at location: " + neuronFInitNcls[i].toString());

            }

        }

        if (nativeBlocks.length()>0)
        {
            addMajorHocComment(response, "Adding blocks of native NEURON code");
            response.append(nativeBlocks.toString());
        }


        return response.toString();
    }


    private String generateInitialParameters()
    {
        StringBuffer response = new StringBuffer();

        addMajorHocComment(response, "Setting initial parameters");

        response.append("strdef simConfig\n");
        response.append("simConfig = \""+this.simConfig.getName()+"\"\n");

        response.append("celsius = " + project.simulationParameters.getTemperature() + "\n\n");

        response.append("proc initialiseValues() {\n\n");

        ArrayList<String> cellGroupNames = project.cellGroupsInfo.getAllCellGroupNames();

        for (int cellGroupIndex = 0; cellGroupIndex < cellGroupNames.size(); cellGroupIndex++)
        {
            String cellGroupName = cellGroupNames.get(cellGroupIndex);

            int numInCellGroup = project.generatedCellPositions.getNumberInCellGroup(cellGroupName);
            if (numInCellGroup > 0)
            {

                String cellType = project.cellGroupsInfo.getCellType(cellGroupName);
                Cell cell = project.cellManager.getCell(cellType);

                String nameOfNumberOfTheseCells = "n_" + cellGroupName;
                String nameOfArrayOfTheseCells = "a_" + cellGroupName;

                ArrayList cellGroupPositions = project.generatedCellPositions.getPositionRecords(cellGroupName);

                addHocComment(response, "Setting initial vals in cell group: " + cellGroupName
                           + " which has " + cellGroupPositions.size() + " cells");

                for (int cellIndex = 0; cellIndex < cellGroupPositions.size(); cellIndex++)
                {
                    PositionRecord posRecord
                        = (PositionRecord) cellGroupPositions.get(cellIndex);

                    if (cell.getInitialPotential().getDistributionType() != NumberGenerator.FIXED_NUM)
                    {
                        double initVolt = UnitConverter.getVoltage(cell.getInitialPotential().getNextNumber(),
                                                                   UnitConverter.NEUROCONSTRUCT_UNITS,
                                                                   UnitConverter.NEURON_UNITS);
                        addHocComment(response,
                                   "Giving cell " + posRecord.cellNumber + " an initial potential of: " + initVolt+" based on: "+ cell.getInitialPotential().toString());

                        if (simConfig.getMpiConf().isParallel()) response.append("  if(isCellOnNode(\""+cellGroupName+"\", "
                                                                        + posRecord.cellNumber + ")) {\n");

                        response.append("    forsec " + nameOfArrayOfTheseCells + "[" + posRecord.cellNumber + "].all {\n");
                        response.append("        v = " + initVolt + "\n");
                        response.append("    }\n\n");
                        
                        if (simConfig.getMpiConf().isParallel()) response.append("  }\n\n");
                    }

                    //Point3f point = new Point3f(posRecord.x_pos, posRecord.y_pos, posRecord.z_pos);

                }

                if (cell.getInitialPotential().getDistributionType() == NumberGenerator.FIXED_NUM &&
                    cellGroupPositions.size() > 0)
                {

                    double initVolt = UnitConverter.getVoltage(cell.getInitialPotential().getNextNumber(),
                                                               UnitConverter.NEUROCONSTRUCT_UNITS,
                                                               UnitConverter.NEURON_UNITS);

                    addHocComment(response, "Giving all cells an initial potential of: " + initVolt);

                    response.append("    for i = 0, " + nameOfNumberOfTheseCells + "-1 {" + "\n");
                    response.append("        ");

                        if (simConfig.getMpiConf().isParallel()) response.append("if(isCellOnNode(\""+cellGroupName
                                                                        +"\", i)) ");

                        response.append("forsec " + nameOfArrayOfTheseCells + "[i].all "
                                    + " v = " + initVolt + "\n\n");
                    response.append("    }" + "\n\n");

                }

                response.append("\n");
            }
        }

        response.append("}\n\n");

        response.append("objref fih\n");
        response.append("fih = new FInitializeHandler(0, \"initialiseValues()\")\n\n\n");

        return response.toString();
    }

    private String generateAccess()
    {
        StringBuffer response = new StringBuffer();
        response.append("\n");

        if (simConfig.getMpiConf().isParallel())
        {
            addHocComment(response, "Cycling through cells and setting access to first one on this node");
            response.append("test_gid = 0\n");
            response.append("while (test_gid < ncell) {\n");
            response.append("    if (pnm.gid_exists(test_gid)) {\n");
           // if (addComments)
            //    response.append("        //print \"Setting access on host \", host, \", host id \", hostid, \", to cell gid: \", test_gid, \":\"\n");
            response.append("        objectvar accessCell\n");
          //  response.append("        //print pnm.pc.gid2cell(test_gid).Soma\n");
            response.append("        test_gid = ncell\n");
            response.append("    } else {\n");
            response.append("        test_gid = test_gid + 1\n");
            response.append("    } \n");
            response.append("}\n");

            return response.toString();
        }


        ArrayList<String> cellGroupNames = project.cellGroupsInfo.getAllCellGroupNames();

        if (cellGroupNames.size() == 0)
        {
            logger.logError("There are no cell groups!!", null);
            return "";
        }
        Cell cellToWatch = null;
        String cellGroupToWatch = null;
        int cellNumToWatch = -1;

        cellGroupToWatch = null;
        int cellGroupCount = 0;
        ArrayList<String> allCellGroups = project.cellGroupsInfo.getAllCellGroupNames();

        while (cellGroupToWatch == null)
        {
            String nextCellGroup = allCellGroups.get(cellGroupCount);

            if (project.generatedCellPositions.getNumberInCellGroup(nextCellGroup) > 0)
            {
                cellGroupToWatch = nextCellGroup;
            }
            cellGroupCount++;
        }

        cellNumToWatch = 0;
        cellToWatch = project.cellManager.getCell(project.cellGroupsInfo.getCellType(cellGroupToWatch));

        //this.addHocFileComment(response, "Accessing cell by it's type.. ");
        response.append("access " + cellToWatch.getInstanceName() + "[" + cellNumToWatch + "]."
                        + getHocSectionName(cellToWatch.getFirstSomaSegment().getSection().getSectionName()));
        response.append("\n");

        return response.toString();
    }

    private String generateRandomise()
    {
        StringBuffer response = new StringBuffer();

        addHocComment(response, "Initializes random-number generator");
        response.append("use_mcell_ran4(1)\n\n");
        if (!simConfig.getMpiConf().isParallel())
        {
            response.append("mcell_ran4_init(" + this.randomSeed + ")\n");
        }
        else
        {
            addHocComment(response, "As the simulation is being run in parallel, initialising differently on each host.\n"
                                +"Adding the hostid to the rand seed allows reproducability of the sim, as long as the\n"
                                +"same network distribution is used.");
            
            response.append("mcell_ran4_init(" + this.randomSeed + " + hostid)\n");
        }
        
        return response.toString();

    }

    private String getHostname()
    {
        StringBuffer response = new StringBuffer();

        if (this.savingHostname()) // temporarily disabled for win, will it ever be needed?
        {
            addHocComment(response, "Getting hostname");
    
            response.append("objref strFuncs\n");
            response.append("strFuncs = new StringFunctions()\n");
    
            response.append("strdef host\n");
    
            if (GeneralUtils.isWindowsBasedPlatform())
                response.append("system(\"C:/WINDOWS/SYSTEM32/hostname.exe\", host)\n");
            else
                response.append("system(\"hostname\", host)\n");
    
            response.append("strFuncs.left(host, strFuncs.len(host)-1)\n\n");
        }
        
        return response.toString();
    }



    private String initialiseParallel()
    {
        StringBuffer response = new StringBuffer();
        
        if (simConfig.getMpiConf().isParallel())
        {
            addMajorHocComment(response, "Initialising parallelization");
    
    
            response.append("ncell = " + project.generatedCellPositions.getNumberInAllCellGroups() + "\n\n");
    
            addHocComment(response, "Parallel NEURON setup");
    
            response.append("load_file(\"netparmpi.hoc\")\n");
            response.append("objref pnm\n");
            response.append("pnm = new ParallelNetManager(ncell)\n\n");
    
            response.append("hostid = pnm.pc.id\n\n");
    
            if (addComments) response.append("print \"Set up ParallelNetManager managing \",ncell,\"cells in total on: \", host, \"with hostid: \", hostid\n");
    
    
            //response.append("pnm.round_robin()\n");
    
            response.append("\n");
        }
        else
        {
            addHocComment(response, "Simulation running in serial mode, setting default host id");
            response.append("hostid = 0\n\n");
        }

        return response.toString();

    }


    private String associateCellsWithNodes()
    {
        StringBuffer response = new StringBuffer();
        
        
        if (!simConfig.getMpiConf().isParallel())
        {
            if (!isRunModePythonBased(genRunMode))
            {
                return ""; // nothing to do
            }
            else
            {

                response.append("\n\nfunc isCellOnNode() {\n");
                response.append("    return 1 // serial mode, so yes...\n");
                response.append("}\n");
                
                return response.toString();
            }
        }

        addMajorHocComment(response, "Associating cells with nodes");

        ArrayList<String> cellGroupNames = simConfig.getCellGroups();

        logger.logComment("Looking at " + cellGroupNames.size() + " cell groups");
        
        MpiConfiguration mpiConfig = simConfig.getMpiConf();

        int totalProcs = mpiConfig.getTotalNumProcessors();
        

        addHocComment(response, "MPI Configuration: "+ mpiConfig.toString().trim());


        response.append("func getCellGlobalId() {\n\n");

        int currentGid = 0;

        for (int cellGroupIndex = 0; cellGroupIndex < cellGroupNames.size(); cellGroupIndex++)
        {
            String cellGroupName = cellGroupNames.get(cellGroupIndex);
            response.append("    if (strcmp($s1,\""+cellGroupName+"\")==0) {\n");

            addHocComment(response, "There are " + project.generatedCellPositions.getNumberInCellGroup(cellGroupName)
                            + " cells in this Cell Group", "        ", false);

            response.append("        cgid = "+currentGid+" + $2\n");
            currentGid+=project.generatedCellPositions.getNumberInCellGroup(cellGroupName);
            response.append("    }\n\n");
        }


        response.append("    return cgid\n");
        response.append("}\n\n");
        
        
        
     

        StringBuffer gidToNodeInfo = new StringBuffer();
        
        for (int cellGroupIndex = 0; cellGroupIndex < cellGroupNames.size(); cellGroupIndex++)
        {
            String cellGroupName = cellGroupNames.get(cellGroupIndex);
            ArrayList<PositionRecord> posRecs = project.generatedCellPositions.getPositionRecords(cellGroupName);

            ////////////response.append("    if (strcmp($s1,\""+cellGroupName+"\")==0) {\n");

            for (PositionRecord pr: posRecs)
            {

                gidToNodeInfo.append("pnm.set_gid2node(getCellGlobalId(\"" + cellGroupName 
                        + "\", " + pr.cellNumber + "), " + pr.getNodeId() + ")\n");
                

                ///////////response.append("        if ($2 == "+pr.cellNumber+") return (hostid == " + pr.nodeId + ")\n");

            }
            ////////////response.append("    }\n\n");
        }
        //////////////response.append("    return 0\n");

        response.append(gidToNodeInfo.toString()+"\n"); 
    
    
        
        

        addHocComment(response, "Returns 0 or 1 depending on whether the gid for cell group $s1, id $2\n"+
                "is on this node i.e. via set_gid2node() or register_cell()", false);
        response.append("func isCellOnNode() {\n\n");
        response.append("    cellgid = getCellGlobalId($s1, $2)\n");

        response.append("    return pnm.gid_exists(cellgid)!=0\n");

        response.append("}\n\n");

        response.append("\n");

        return response.toString();

    }
 /*
    private String runworkerCutoff()
    {

        StringBuffer response = new StringBuffer();

        this.addComment(response,
                               "Everything before this will be run by all workers, after, only the master runs");

        response.append("pnm.pc.runworker()\n");
        ArrayList<String> cellGroupNames = project.cellGroupsInfo.getAllCellGroupNames();
        logger.logComment("Looking at " + cellGroupNames.size() + " cell groups");

        if (cellGroupNames.size() == 0)
        {
            logger.logComment("There are no cell groups!!");

            addMajorComment(response, "There were no cell groups specified in the project...");
            return response.toString();
        }
        for (int ii = 0; ii < cellGroupNames.size(); ii++)
        {
            String cellGroupName = cellGroupNames.get(ii);

            ArrayList cellGroupPositions = project.generatedCellPositions.getPositionRecords(cellGroupName);
            if (project.generatedCellPositions.getNumberInCellGroup(cellGroupName) == 0)
            {
                logger.logComment("No cells generated in that group. Ignoring...");
            }
            else
            {

                String cellTypeName = project.cellGroupsInfo.getCellType(cellGroupName);

                addComment(response, "Adding " + cellGroupPositions.size()
                                  + " cells of type " + cellTypeName);

                String nameOfNumberOfTheseCells = "n_" + cellGroupName;

                response.append("for i=0, " + nameOfNumberOfTheseCells + " - 1 {\n");

                response.append("pnm.pc.submit(\"addCell_" + cellGroupName + "\", i)\n");

                response.append("}\n");
            }
        }
        response.append("while (pnm.pc.working) {\n");

        response.append("ret =  pnm.pc.retval    // the return value for the executed function\n");

        response.append("print \"Returned value: \", ret\n");

        response.append("}\n");

        return response.toString();


    }*/

    private String finishParallel()
    {
        StringBuffer response = new StringBuffer();

        addHocComment(response, "Shutting down parallelisation");

        ///response.append("forall psection()\n");
        response.append("\n");
        response.append("\n");
        response.append("\n");


        response.append("pnm.pc.done\n");
       // response.append("quit()\n");



        return response.toString();

    }

    public long getCurrentRandomSeed()
    {
        return this.randomSeed;
    }

    public long getCurrentRunMode()
    {
        return this.genRunMode;
    }

    public float getCurrentGenTime()
    {
        return genTime;
    }

    private String generateWelcomeComments()
    {
        StringBuffer response = new StringBuffer();
        if (!project.neuronSettings.isGenerateComments()) return "";
        
        /*if (simConfig.getMpiConf().isParallel())
        {
            response.append("if (hostid == 0) {\n");
        }*/

        String indent = "    ";

        response.append("print \"\"\n");
        response.append("print \"*****************************************************\"\n");
        response.append("print \"\"\n");
        response.append("print \""+indent+"neuroConstruct generated NEURON simulation \"\n");
        response.append("print \""+indent+"for project: " + project.getProjectFile().getAbsolutePath() + " \"\n");
      
        response.append("print \"\"\n");



        String desc = new String();

        if (project.getProjectDescription() != null) desc = project.getProjectDescription();
        
        desc = GeneralUtils.replaceAllTokens(desc, "\"", "");
        desc = GeneralUtils.replaceAllTokens(desc, "\n", "\"\nprint \""+indent);
        
        

        response.append("print \""+indent+"Description: " + desc + "\"\n");
        
          response.append("print \"\"\n");
        response.append("print \""+indent+"Simulation Configuration: " + simConfig + " \"\n");
        if (simConfig.getDescription().trim().length()>0)
        {
            desc = simConfig.getDescription();
            
            response.append("print \""+indent + desc + " \"\n");
        }

        response.append("print \" \"\n");
        response.append("print  \"*****************************************************\"\n\n");
        
        if (GeneralUtils.isLinuxBasedPlatform() || GeneralUtils.isMacBasedPlatform())
        {
            response.append("strdef pwd\n");
    
            response.append("system(\"pwd\", pwd)\n");
            
            response.append("print \"Current working dir: \", pwd\n\n");
        }
        
        /*if (simConfig.getMpiConf().isParallel())
        {
            response.append("}\n");
        }*/
        return response.toString();
    };

    private String generateHocIncludes()
    {
        StringBuffer response = new StringBuffer();

        response.append("objectvar allCells\n");
        response.append("allCells = new List()\n\n");

        addHocComment(response, "A flag to signal simulation was generated by neuroConstruct ");
        response.append("nC = 1\n\n");
        
        addHocComment(response, "Including neuroConstruct utilities file ");
        response.append("load_file(\""+utilsFile.getName()+"\")\n");
        addHocComment(response, "Including neuroConstruct cell check file ");
        response.append("load_file(\""+cellCheckFile.getName()+"\")\n");

        return response.toString();
    }
    
    
    private String generatePythonRunFile()
    {
        StringBuffer response = new StringBuffer();
        
        response.append("import neuron\n");
        response.append("from neuron import hoc\n");
        response.append("import nrn\n\n");
        
        
        PythonUtils.addComment(response, "Note: As neuroConstruct already generates hoc, much of this is reused and not (yet) converted \n" +
                "to pure Python. It is mainly the cell and network creation that will benefit from the Python parsing of XML/HDF5", addComments);
        
        response.append("hoc.execute('load_file(\""+project.getProjectName()+".hoc\")')\n");
        
        return response.toString();
    }


    private String generatePythonIncludes()
    {
        StringBuffer response = new StringBuffer();


        PythonUtils.addComment(response, "Including some standard Python modules ", addComments);
        
        response.append("import sys\n");
        response.append("import os\n\n");
        
        response.append("import xml.sax\n");
        response.append("import time\n\n");

        response.append("import logging\n\n");


        PythonUtils.addComment(response, "Adding working dir to Python path", addComments);
        
        response.append("if sys.path.count(os.getcwd())==0: sys.path.append(os.getcwd())\n\n");

        PythonUtils.addComment(response, "Including NEURON specifics", addComments);
        
        
        response.append("import neuron\n");
        response.append("from neuron import hoc\n");
        response.append("import nrn\n\n");
        

        response.append("sys.path.append(\"NeuroMLUtils\")\n");
        response.append("sys.path.append(\"NEURONUtils\")\n\n");

        if (genRunMode== RUN_PYTHON_XML) 
            response.append("import NetworkMLSaxHandler\n");
        
        if (genRunMode== RUN_PYTHON_HDF5) 
            response.append("import NetworkMLHDF5Handler\n");
        
        response.append("import NEURONSimUtils\n\n");



        //response.append("from NetworkMLSaxHandler import NetworkMLSaxHandler\n");
        //response.append("from NetworkHandler import NetworkHandler\n\n");

        return response.toString();
    }

    
    private String initialisePythonLogging()
    {
        StringBuffer response = new StringBuffer();
        
        String logLevel = "WARN";
        if (addComments) logLevel ="INFO";

        response.append("logformat = \"%(name)-19s %(levelname)-5s -\"+str(int(h.hostid))+\"- %(message)s\"\n");

        response.append("logging.basicConfig(level=logging."+logLevel+", format=logformat)\n");
        
        response.append("log = logging.getLogger(\""+project.getProjectName()+"\")\n\n");
        
        return response.toString();
    }
    

    private String initialPythonSetup()
    {
        StringBuffer response = new StringBuffer();
        
        response.append("h = hoc.HocObject()\n\n");
        
        /*
        if (simConfig.getMpiConf().isParallel())
        {
            response.append("h.load_file(\"netparmpi.hoc\")\n");
            
            response.append("h(\"objref pnm\")\n");
            response.append("h(\"pnm = new ParallelNetManager(20)\")\n");
            
            response.append("h(\"hostid = pnm.pc.id\")\n");

            response.append("print \"My host id: %d\" % (h.hostid)\n\n");
        }
        else
        {
            response.append("h(\"hostid = 0\")\n");
        }*/
        
        return response.toString();
    }
    
    
    /*
     * Only used when Python is script...
     * 
     */
    private String loadNetworkMLStructure()
    {
        StringBuffer response = new StringBuffer();
        
        String nmlFile = null;
        
        if (genRunMode== RUN_PYTHON_XML)
        {
            nmlFile = NetworkMLConstants.DEFAULT_NETWORKML_FILENAME_XML;
        } 
        else if (genRunMode == RUN_PYTHON_HDF5)
        {
            nmlFile = NetworkMLConstants.DEFAULT_NETWORKML_FILENAME_HDF5;
        }
        
        PythonUtils.addPrintedComment(response, 
                "Loading cell positions and connections from: "+ nmlFile, 
                PythonUtils.LOG_LEVEL_INFO,
                true);

        response.append("file_name = '"+nmlFile+"'\n\n");

        response.append("beforeLoad = time.time()\n");
        
        response.append("nmlHandler = NEURONSimUtils.NetManagerNEURON()\n");
        
        if (genRunMode== RUN_PYTHON_XML)
        {
            response.append("parser = xml.sax.make_parser()\n");   

            response.append("curHandler = NetworkMLSaxHandler.NetworkMLSaxHandler(nmlHandler)\n");

            if (simConfig.getMpiConf().isParallel())
                response.append("curHandler.setNodeId(h.hostid)\n");
            else
                response.append("curHandler.setNodeId(-1) \n");
                

            response.append("parser.setContentHandler(curHandler)\n");

            response.append("parser.parse(open(file_name)) \n");
        }
        else if (genRunMode == RUN_PYTHON_HDF5)
        {
            response.append("curHandler = NetworkMLHDF5Handler.NetworkMLHDF5Handler(nmlHandler)\n");

            if (simConfig.getMpiConf().isParallel())
                response.append("curHandler.setNodeId(h.hostid)\n");
            else
                response.append("curHandler.setNodeId(-1) \n");


            response.append("curHandler.parse(file_name) \n");
        }
        

        response.append("afterLoad = time.time()\n");

        //response.append("comment = time.time()\n");
              
        
        PythonUtils.addPrintedComment(response,
                "\"Loaded file in \"+ str(afterLoad-beforeLoad)+ \" seconds on host: %d\" % (int(h.hostid))", 
                PythonUtils.LOG_LEVEL_INFO, false);
        
        
        return response.toString();
    }
    
    
    
    
    
    
    
    
    
    
    
    
    

    private String generateRunSettings()
    {
        StringBuffer response = new StringBuffer();
        addMajorHocComment(response, "Settings for running the demo");
        response.append("\n");
        response.append("tstop = " + getSimDuration() + "\n");

        /** @todo See why this is necessary (dt = 0.1 etc wasn't enough...) */
        response.append("dt = " + project.simulationParameters.getDt() + "\n");
        //response.append("steps_per_ms = " + Math.round(1d / (double) project.simulationParameters.getDt()) + "\n");
        response.append("steps_per_ms = " + 1d / (double) project.simulationParameters.getDt() + "\n");

        if (simConfig.getMpiConf().isParallel())
        {
            response.append("pnm.set_maxstep(5)\n\n");
            return response.toString();
        }


        return response.toString();
    }

    private float getSimDuration()
    {
        if (simConfig.getSimDuration() == 0) // shouldn't be...
        {
            return project.simulationParameters.getDuration();
        }
        else
            return simConfig.getSimDuration();
    }

    private String getStimArrayName(String stimRef)
    {
        return "stim_" + stimRef;
    }

    private String getStimArraySizeName(String stimRef)
    {
        return "n_stim_" + stimRef;
    }

    private String generateStimulations() throws NeuronException
    {
        int totalStims = project.generatedElecInputs.getNumberSingleInputs();

        StringBuffer response = new StringBuffer(totalStims*800);  // initial cap

        ArrayList<String> allStims = this.simConfig.getInputs();

        addMajorHocComment(response, "Adding " + allStims.size() + " stimulation(s)");

        for (int k = 0; k < allStims.size(); k++)
        {
            //StimulationSettings nextStim = project.generatedElecInputs.getStim();

            logger.logComment("++++++++++++     Checking for stim ref: " + allStims.get(k));

            ArrayList<SingleElectricalInput> allInputLocs =
                project.generatedElecInputs.getInputLocations(allStims.get(k));

            if (allInputLocs.size() > 0)
            {
                logger.logComment("Going to add stim to " + allInputLocs.size() + " cells in input group: " +
                                  allStims.get(k));

                for (int j = 0; j < allInputLocs.size(); j++)
                {
                    SingleElectricalInput nextInput = allInputLocs.get(j);

                    if (!project.cellGroupsInfo.getAllCellGroupNames().contains(nextInput.getCellGroup()))
                    {
                        throw new NeuronException("The Cell Group specified for the Stimulation: " + allStims.get(k) +
                                                  " does not exist!");
                    }

                    String stimCellType = project.cellGroupsInfo.getCellType(nextInput.getCellGroup());
                    Cell stimCell = project.cellManager.getCell(stimCellType);

                    Segment segToStim = stimCell.getSegmentWithId(nextInput.getSegmentId());
                    
                    logger.logComment("Going to add stim to seg " + nextInput.getSegmentId() + ": "+ segToStim);

                    float fractionAlongSegment = nextInput.getFractionAlong();

                    float fractionAlongSection
                        = CellTopologyHelper.getFractionAlongSection(stimCell,
                                                                     segToStim,
                                                                     fractionAlongSegment); // assume centre of segment...

                    if (nextInput.getElectricalInputType().equals(IClamp.TYPE))
                    {
                        String stimObjectFilename = ProjectStructure.getModTemplatesDir().getAbsolutePath()+"/"+ EXT_CURR_CLAMP_MOD;

                        if (!stimModFilesRequired.contains(stimObjectFilename))
                        {
                            stimModFilesRequired.add(stimObjectFilename);

                            try
                            {
                                File soFile = new File(stimObjectFilename);
                                long lastMod = soFile.lastModified();
                                    
                                File copied = GeneralUtils.copyFileIntoDir(soFile,
                                                             ProjectStructure.getNeuronCodeDir(project.getProjectMainDirectory()));
                                
                                copied.setLastModified(lastMod);
                                
                            }
                            catch(IOException io)
                            {
                                GuiUtils.showErrorMessage(logger, "Problem copying mod file for stimulation: " + stimObjectFilename, io, null);
                                return null;
                            }
                        }

                        String stimName = getStimArrayName(allStims.get(k));

                        IClampSettings iClamp = (IClampSettings) project.elecInputInfo.getStim(allStims.get(k));

                        logger.logComment("Adding stim: " + nextInput);

                        if (j == 0) // define array...
                        {
                            String sizeName = getStimArraySizeName(allStims.get(k));
                            response.append(sizeName + " = " + allInputLocs.size() + "\n");
                            response.append("objectvar " + stimName + "[" + sizeName + "]\n\n");
                        }

                        String prefix = "";
                        String post = "";

                        if (simConfig.getMpiConf().isParallel())
                        {
                            prefix = "    ";
                            post = "}" + "\n";
                            response.append("if (isCellOnNode(\""
                                            + nextInput.getCellGroup() + "\", "
                                            + nextInput.getCellNumber() + ")) {\n");
                        }

                        addHocComment(response, "Note: the stimulation was specified as being at a point "
                                          + fractionAlongSegment + " along segment: " + segToStim.getSegmentName(),prefix, false);
                        addHocComment(response, "in section: " + getHocSectionName(segToStim.getSection().getSectionName()) +
                                          ". For NEURON, this translates to a point " + fractionAlongSection +
                                          " along section: " +
                                          getHocSectionName(segToStim.getSection().getSectionName()),prefix,true);

                        response.append(prefix+"a_" + nextInput.getCellGroup()
                                        + "[" + nextInput.getCellNumber() + "]"
                                        + "." + getHocSectionName(segToStim.getSection().getSectionName()) + " {\n");

                        String stimObjectName = EXT_CURR_CLAMP_MOD.substring(0, EXT_CURR_CLAMP_MOD.indexOf(".mod"));
                        
                        response.append(prefix+"    "+stimName + "[" + j + "] = new "+stimObjectName+"(" +
                                        fractionAlongSection +
                                        ")\n");

                        response.append(prefix+"    "+stimName + "[" + j + "].del = " + iClamp.getDelay().getStart() + "\n");
                        response.append(prefix+"    "+stimName + "[" + j + "].dur = " + iClamp.getDuration().getStart() + "\n");
                        response.append(prefix+"    "+stimName + "[" + j + "].amp = " + iClamp.getAmplitude().getStart() + "\n");

                        int repeat = iClamp.isRepeat() ? 1:0;

                        response.append(prefix+"    "+stimName + "[" + j + "].repeat = " + repeat + "\n");

                        response.append(prefix+"}" + "\n");
                        response.append(post);
                        response.append("\n");

                    }
                    else if (nextInput.getElectricalInputType().equals(RandomSpikeTrain.TYPE))
                    {
                        // to make the NetStim more randomish...
                        int increaseFactor = 100;
                        float noise = 1f;

                        RandomSpikeTrainSettings rndTrain =
                            (RandomSpikeTrainSettings) project.elecInputInfo.getStim(allStims.get(k));

                        logger.logComment("Adding stim: " + nextInput);

                        String stimName = "spikesource_" + allStims.get(k);
                        String synapseName = "synapse_" + allStims.get(k);
                        String connectionName = "connection_" + allStims.get(k);

                        if (j == 0) // define arrays...
                        {
                            response.append("objref " + stimName + "[" + allInputLocs.size() + "]\n\n");
                            response.append("objref " + synapseName + "[" + allInputLocs.size() + "]\n");
                            response.append("objref " + connectionName + "[" + allInputLocs.size() + "]\n");
                            response.append("thresh = -20\n");
                            response.append("delay = 0\n");
                            response.append("weight = 1\n\n");

                        }

                        /*  This is right!!!!
                                                  response.append("access a_"
                                        + nextInput.getCellGroup()
                                        + "["
                                        + nextInput.getCellNumber()
                                        + "]." + getHocSectionName(segToStim.getSection().getSectionName()) + " \n");
                         */



                        String prefix = "";
                        String post = "";

                        if (simConfig.getMpiConf().isParallel())
                        {
                            prefix = "    ";
                            post = "}" + "\n";
                            response.append("if (isCellOnNode(\""
                                            + nextInput.getCellGroup() + "\", "
                                            + nextInput.getCellNumber() + ")) {\n");
                        }

                        response.append(prefix+"access "
                                        + "a_" + nextInput.getCellGroup()
                                        + "["
                                        + nextInput.getCellNumber()
                                        + "]." + getHocSectionName(segToStim.getSection().getSectionName()) + " \n");

                        response.append(prefix+stimName + "[" + j + "] = new NetStim(" +
                                        fractionAlongSection + ")\n");

                        /** @todo This is wrong!!! */
                        float expectedRate = rndTrain.getRate().getNextNumber();

                        addHocComment(response,
                                          "NOTE: This is a very rough way to get an average rate of " + expectedRate +
                                          " kHz!!!", prefix, false);

                        float expectedNumber = getSimDuration()
                            * expectedRate
                            * increaseFactor; // no units...

                        double interval = UnitConverter.getTime(1f / expectedRate,
                                                                UnitConverter.NEUROCONSTRUCT_UNITS,
                                                                UnitConverter.NEURON_UNITS);

                        response.append(prefix+stimName + "[" + j + "].number = " + expectedNumber +
                                        "\n");
                        response.append(prefix+stimName + "[" + j + "].interval = " + interval + "\n");

                        response.append(prefix+stimName + "[" + j + "].noise = " + noise + " \n");
                        response.append(prefix+stimName + "[" + j + "].start = 0 \n");

                        response.append(prefix+synapseName + "[" + j + "] = new " +
                                        rndTrain.getSynapseType() +
                                        "(" + fractionAlongSection +
                                        ") \n");

                        addHocComment(response, " Inserts synapse 0.5 of way down",prefix, true);

                        response.append(prefix+connectionName + "["
                                        + j
                                        + "] = new NetCon("
                                        + stimName + "["
                                        + j +
                                        "], " + synapseName + "["
                                        + j +
                                        "], thresh, delay, weight)\n");

                        response.append(post);
                        response.append("\n\n");

                    }

                    else if (nextInput.getElectricalInputType().equals(RandomSpikeTrainExt.TYPE))
                    {

                        String stimObjectName = "NetStimExt";
                        String stimObjectFilename = ProjectStructure.getModTemplatesDir().getAbsolutePath()+"/"+ stimObjectName + ".mod";

                        if (!stimModFilesRequired.contains(stimObjectFilename))
                        {
                            stimModFilesRequired.add(stimObjectFilename);

                            try
                            {
                                GeneralUtils.copyFileIntoDir(new File(stimObjectFilename),
                                                             ProjectStructure.getNeuronCodeDir(project.getProjectMainDirectory()));
                            }
                            catch(IOException io)
                            {
                                GuiUtils.showErrorMessage(logger, "Problem copying mod file for stimulation: " + stimObjectFilename, io, null);
                                return null;
                            }
                        }



                        // to make the NetStim more randomish...
                        int increaseFactor = 100;
                        float noise = 1f;

                        RandomSpikeTrainExtSettings rndTrainExt =
                            (RandomSpikeTrainExtSettings) project.elecInputInfo.getStim(allStims.get(k));

                        logger.logComment("Adding stim: " + nextInput);

                        String stimName = "spikesource_" + allStims.get(k);
                        String synapseName = "synapse_" + allStims.get(k);
                        String connectionName = "connection_" + allStims.get(k);

                        if (j == 0) // define arrays...
                        {
                            response.append("objref " + stimName + "[" + allInputLocs.size() + "]\n\n");
                            response.append("objref " + synapseName + "[" + allInputLocs.size() + "]\n");
                            response.append("objref " + connectionName + "[" + allInputLocs.size() + "]\n");
                            response.append("thresh = -20\n");
                            response.append("delay = 0\n");
                            response.append("weight = 1\n\n");

                        }

                        /*  This is right!!!!
                                                  response.append("access a_"
                                        + nextInput.getCellGroup()
                                        + "["
                                        + nextInput.getCellNumber()
                                        + "]." + getHocSectionName(segToStim.getSection().getSectionName(() + " \n");
                         */



                        String prefix = "";
                        String post = "";

                        if (simConfig.getMpiConf().isParallel())
                        {
                            prefix = "    ";
                            post = "}" + "\n";
                            response.append("if (isCellOnNode(\""
                                            + nextInput.getCellGroup() + "\", "
                                            + nextInput.getCellNumber() + ")) {\n");
                        }

                        response.append(prefix+"access "
                                        + "a_" + nextInput.getCellGroup()
                                        + "["
                                        + nextInput.getCellNumber()
                                        + "]." + getHocSectionName(segToStim.getSection().getSectionName()) + " \n");

                        response.append(prefix+stimName + "[" + j + "] = new "+stimObjectName+"(" +
                                        fractionAlongSection + ")\n");

                        /** @todo This is wrong!!! */
                        float expectedRate = rndTrainExt.getRate().getNextNumber();

                        addHocComment(response,
                                          "NOTE: This is a very rough way to get an average rate of " + expectedRate +
                                          " kHz!!!", prefix, false);

                        float expectedNumber = getSimDuration()
                            * expectedRate
                            * increaseFactor; // no units...

                        double interval = UnitConverter.getTime(1f / expectedRate,
                                                                UnitConverter.NEUROCONSTRUCT_UNITS,
                                                                UnitConverter.NEURON_UNITS);

                        response.append(prefix+stimName + "[" + j + "].number = " + expectedNumber +
                                        "\n");
                        response.append(prefix+stimName + "[" + j + "].interval = " + interval + "\n");

                        response.append(prefix+stimName + "[" + j + "].noise = " + noise + " \n");
                        response.append(prefix+stimName + "[" + j + "].del = "+ rndTrainExt.getDelay() +" \n");
                        response.append(prefix+stimName + "[" + j + "].dur = "+ rndTrainExt.getDuration() +" \n");

                        int repeat = rndTrainExt.isRepeat() ? 1:0;

                        response.append(prefix+stimName + "[" + j + "].repeat = "+ repeat +" \n");

                        response.append(prefix+synapseName + "[" + j + "] = new " +
                                        rndTrainExt.getSynapseType() +
                                        "(" + fractionAlongSection +
                                        ") \n");

                        addHocComment(response, " Inserts synapse 0.5 of way down",prefix, true);

                        response.append(prefix+connectionName + "["
                                        + j
                                        + "] = new NetCon("
                                        + stimName + "["
                                        + j +
                                        "], " + synapseName + "["
                                        + j +
                                        "], thresh, delay, weight)\n");

                        response.append(post);
                        response.append("\n\n");

                    }

                }
            }
            else
            {
                addHocComment(response, "No electrical inputs generated for: " + allStims.get(k));
            }
        }

        return response.toString();
    }

    private String generateMultiRunPreScript()
    {
        StringBuffer response = new StringBuffer();

        if (multiRunManager!=null)
        {
            response.append(multiRunManager.getMultiRunPreScript(SimEnvHelper.NEURON));
        }

        return response.toString();
    }

    private String generateMultiRunPostScript()
    {
        StringBuffer response = new StringBuffer();

        if (multiRunManager!=null)
        {
            response.append(multiRunManager.getMultiRunPostScript(SimEnvHelper.NEURON));
        /*
                for (String nextLoop: multiRunLoops)
                {
                    this.addHocFileComment( response,"End of loop for: "+nextLoop);
                    response.append("}\n\n");
                }*/
        }

        return response.toString();
    }


    public static String getHocSectionName(String secname)
    {
        String newName = GeneralUtils.replaceAllTokens(secname,
                ".",
                "_");
        newName = GeneralUtils.replaceAllTokens(newName,
                "[",
                "_");
        newName = GeneralUtils.replaceAllTokens(newName,
                "]",
                "_");
        
        return newName;
    }
    

    public static String getHocSegmentName(String secname)
    {
        return getHocSectionName(secname);
    }

    public static String getHocFriendlyFilename(String filename)
    {
        logger.logComment("filename: " + filename);
        filename = GeneralUtils.replaceAllTokens(filename, "\\", "/");

        filename = GeneralUtils.replaceAllTokens(filename,
                                                 "Program Files",
                                                 "Progra~1");

        filename = GeneralUtils.replaceAllTokens(filename,
                                                 "Documents and Settings",
                                                 "Docume~1");

        if (GeneralUtils.isWindowsBasedPlatform())
        {
            boolean canFix = true;
            // Can catch spaces if a dir is called c:\Padraig Gleeson and change it to c:\Padrai~1
            while (filename.indexOf(" ") > 0 && canFix)
            {
                int indexOfSpace = filename.indexOf(" ");

                int prevSlash = filename.substring(0,indexOfSpace).lastIndexOf("/");
                int nextSlash = filename.indexOf("/", indexOfSpace);

                String spacedWord = filename.substring(prevSlash+1, nextSlash);

                logger.logComment("spacedWord: " + spacedWord);

                if (spacedWord.indexOf(" ")<6) canFix = false;
                else
                {
                    String shortened = spacedWord.substring(0,6)+"~1";
                    filename = GeneralUtils.replaceAllTokens(filename, spacedWord, shortened);
                    logger.logComment("filename now: " + filename);
                }
            }
        }


        logger.logComment("filename now: " + filename);

        return filename;

    }

    /**
     *
     * Creates the vectors to store the data generated, runs the simulation, and writes the data to file
     * @param runMode int The run mode
     * @return String for hoc file
     */
    private String generateNeuronSimulationRecording()
    {
        StringBuffer response = new StringBuffer();

        response.append("\n");

        int numStepsTotal = Math.round(getSimDuration() / project.simulationParameters.getDt()) + 1;

        addMajorHocComment(response,
                        "This will run a full simulation of " + numStepsTotal +
                        " steps when the hoc file is executed");

        ArrayList<PlotSaveDetails> recordings = project.generatedPlotSaves.getSavedPlotSaves();

        addHocComment(response, "Recording " + recordings.size() + " variable(s)");

        boolean recordingSomething = !recordings.isEmpty();


        response.append("objref v_time\n");
        response.append("objref f_time\n");
        response.append("objref propsFile\n\n");

        if (recordingSomething)
        {
            String prefix = "";
            String post = "";

            if (simConfig.getMpiConf().isParallel())
            {
                prefix = "    ";
                post = "}" + "\n";

                response.append("if (hostid == 0) {\n");
            }

            response.append(prefix+"v_time = new Vector()\n");
            response.append(prefix+"v_time.record(&t)\n");
            response.append(prefix+"v_time.resize(" + numStepsTotal + ")\n");

            response.append(prefix+"f_time = new File()\n");

            response.append(post);
            response.append("\n");


        }

        for (PlotSaveDetails record : recordings)
        {
            String cellGroupName = record.simPlot.getCellGroup();

            int numInCellGroup = project.generatedCellPositions.getNumberInCellGroup(cellGroupName);

            String cellType = project.cellGroupsInfo.getCellType(cellGroupName);
            Cell cell = project.cellManager.getCell(cellType);

            String whatToRecord = convertToNeuronVarName(record.simPlot.getValuePlotted());

            if (whatToRecord==null) return null;

            boolean isSpikeRecording = record.simPlot.getValuePlotted().indexOf(SimPlot.SPIKE) >= 0;

            if (numInCellGroup > 0)
            {
                addHocComment(response, record.getDescription(true));

                for (Integer segId : record.segIdsToPlot)
                {
                    Segment segToRecord = cell.getSegmentWithId(segId);

                    float lenAlongSection
                        = CellTopologyHelper.getFractionAlongSection(cell,
                                                                     segToRecord,
                                                                     0.5f);

                    if (record.allCellsInGroup && !record.simPlot.isSynapticMechanism())
                    {
                        addHocComment(response,
                                   "Creating vector for segment: " + segToRecord.getSegmentName() + "(ID: " +
                                   segToRecord.getSegmentId() + ")");

                        String objName = this.getObjectName(record, -1, getHocSegmentName(segToRecord.getSegmentName()));

                        if (isSpikeRecording) objName = objName + "_spike";

                        String vectorObj = "v_" + objName;
                        String fileObj = "f_" + objName;
                        String apCountObj = "apc_" + objName;
                        
                        vectorObj = GeneralUtils.replaceAllTokens(vectorObj, ".", "_");
                        fileObj = GeneralUtils.replaceAllTokens(fileObj, ".", "_");

                        response.append("objref " + vectorObj + "[" + numInCellGroup + "]\n");
                        if (isSpikeRecording) response.append("objref " + apCountObj + "[" + numInCellGroup + "]\n");

                        response.append("for i=0, " + (numInCellGroup - 1) + " {\n");

                        String prefix = "";
                        String post = "";

                        if (simConfig.getMpiConf().isParallel())
                        {
                            prefix = "    ";
                            post = "    }" + "\n";
                            response.append("    if (isCellOnNode(\""
                                            + cellGroupName + "\", i)) {\n");
                        }


                        response.append(prefix+"    " + vectorObj + "[i] = new Vector()\n");

                        if (!isSpikeRecording)
                        {
                            response.append(prefix+"    " + vectorObj + "[i].record(&a_" + cellGroupName + "[i]"
                                            + "." + getHocSectionName(segToRecord.getSection().getSectionName()) + "." + whatToRecord + "(" +
                                            lenAlongSection +
                                            "))\n");
                            response.append(prefix+"    " + vectorObj + "[i].resize(" + numStepsTotal + ")\n");
                        }
                        else
                        {
                            response.append(prefix+"    a_" + cellGroupName + "[i]"
                                            + "." + getHocSectionName(segToRecord.getSection().getSectionName()) + " " + apCountObj
                                            + "[i] = new APCount(" + lenAlongSection + ")\n");

                            if (record.simPlot.getValuePlotted().indexOf(SimPlot.PLOTTED_VALUE_SEPARATOR) > 0)
                            {
                                String threshold = record.simPlot.getValuePlotted().substring(record.simPlot.
                                    getValuePlotted().indexOf(SimPlot.PLOTTED_VALUE_SEPARATOR) + 1);
                                response.append(prefix+"    " + apCountObj + "[i].thresh = " + threshold + "\n");

                            }
                            else
                            {
                                response.append(prefix+"    " + apCountObj + "[i].thresh = " + SimPlot.DEFAULT_THRESHOLD + "\n");
                            }
                            response.append(prefix+"    " + apCountObj + "[i].record(" + vectorObj + "[i])\n");

                        }

                        response.append(post);
                        response.append("}\n");

                        response.append("objref " + fileObj + "[" + numInCellGroup + "]\n\n");
                    }
                    else
                    {
                        for (Integer cellNum : record.cellNumsToPlot)
                        {
                            if (record.simPlot.isSynapticMechanism())
                            {
                                String neuronVar = this.convertToNeuronVarName(record.simPlot.getValuePlotted());

                                String netConn = SimPlot.getNetConnName(record.simPlot.getValuePlotted());
                                String synType = SimPlot.getSynapseType(record.simPlot.getValuePlotted());

                                /** @todo Make more efficient, as most synObjs for seg ids will be empty... */

                                ArrayList<PostSynapticObject> synObjs = project.generatedNetworkConnections.getSynObjsPresent(netConn, synType, cellNum, segId);

                                logger.logComment("Syn objs for: " + netConn + ", " + synType + ", cellNum: "
                                                  + cellNum + ", segId: " + segId + ": " + synObjs);

                                for (PostSynapticObject synDetail : synObjs)
                                {
                                    String synObjName = this.getSynObjName(synDetail);

                                    String var = synObjName + "." + neuronVar;

                                    String vectorObj = "v_" + synObjName+"_"+neuronVar;
                                    String fileObj = "f_" + synObjName+"_"+neuronVar;
                                    vectorObj = GeneralUtils.replaceAllTokens(vectorObj, ".", "_");
                                    fileObj = GeneralUtils.replaceAllTokens(fileObj, ".", "_");

                                    response.append("objref " + vectorObj + "\n");

                                    String prefix = "";
                                    String post = "";

                                    if (simConfig.getMpiConf().isParallel())
                                    {
                                        prefix = "    ";
                                        post = "}" + "\n";
                                        response.append("if (isCellOnNode(\""
                                                        + cellGroupName + "\", " + cellNum + ")) {\n");
                                    }

                                    response.append(prefix + vectorObj + " = new Vector()\n");

                                    response.append(prefix + vectorObj + ".record(&"+var+")\n");

                                    response.append(prefix + vectorObj + ".resize(" + numStepsTotal + ")\n");

                                    response.append(post);
                                    response.append("objref " + fileObj + "\n");
                                    response.append("\n");
                                }
                            }
                            else
                            {

                                addHocComment(response,
                                           "Creating vector for segment: " + segToRecord.getSegmentName() +
                                           "(ID: " + segToRecord.getSegmentId() + ") in cell number: " + cellNum);

                                String objName = this.getObjectName(record, cellNum, getHocSegmentName(segToRecord.getSegmentName()));

                                if (isSpikeRecording) objName = objName + "_spike";

                                String vectorObj = "v_" + objName;
                                String fileObj = "f_" + objName;

                                vectorObj = GeneralUtils.replaceAllTokens(vectorObj, ".", "_");
                                fileObj = GeneralUtils.replaceAllTokens(fileObj, ".", "_");
                                    
                                String apCountObj = "apc_" + objName;

                                response.append("objref " + vectorObj + "\n");

                                String prefix = "";
                                String post = "";

                                if (simConfig.getMpiConf().isParallel())
                                {
                                    prefix = "    ";
                                    post = "}" + "\n";
                                    response.append("if (isCellOnNode(\""
                                                    + cellGroupName + "\", " + cellNum + ")) {\n");
                                }

                                if (isSpikeRecording) response.append(prefix + "objref " + apCountObj + "\n");

                                response.append(prefix + vectorObj + " = new Vector()\n");

                                if (!isSpikeRecording)
                                {
                                    response.append(prefix + vectorObj + ".record(&a_" + cellGroupName + "[" + cellNum +
                                                    "]"
                                                    + "." + getHocSectionName(segToRecord.getSection().getSectionName()) + "." +
                                                    whatToRecord + "(" + lenAlongSection + "))\n");

                                    response.append(prefix + vectorObj + ".resize(" + numStepsTotal + ")\n");
                                }
                                else
                                {
                                    response.append(prefix + "    a_" + cellGroupName + "[" + cellNum + "]"
                                                    + "." + getHocSectionName(segToRecord.getSection().getSectionName()) + " " +
                                                    apCountObj
                                                    + " = new APCount(" + lenAlongSection + ")\n");

                                    if (record.simPlot.getValuePlotted().indexOf(SimPlot.PLOTTED_VALUE_SEPARATOR) > 0)
                                    {
                                        String threshold = record.simPlot.getValuePlotted().substring(record.simPlot.
                                            getValuePlotted().indexOf(SimPlot.
                                                                      PLOTTED_VALUE_SEPARATOR) + 1);

                                        response.append(prefix + "    " + apCountObj + ".thresh = " + threshold + "\n");
                                    }
                                    else
                                    {
                                        response.append(prefix + "    " + apCountObj + ".thresh = " +
                                                        SimPlot.DEFAULT_THRESHOLD + "\n");
                                    }

                                    response.append(prefix + "    " + apCountObj + ".record(" + vectorObj + ")\n");

                                }

                                response.append(post);
                                response.append("objref " + fileObj + "\n");
                                response.append("\n");
                            }
                        }

                    }
                }

            }

        }

        File dirForSims = ProjectStructure.getSimulationsDir(project.getProjectMainDirectory());

        String dataFileDirName = dirForSims.getAbsolutePath() + System.getProperty("file.separator");

        String hocFriendlyDirName = getHocFriendlyFilename(dataFileDirName);

        response.append("\n\nstrdef simsDir\n");
        response.append("simsDir = \"" + hocFriendlyDirName + "\"\n\n");

        response.append("strdef simReference\n");
        response.append("simReference = \"" + project.simulationParameters.getReference() + "\"\n\n");

        addHocComment(response, "Note: to change location of the generated simulation files, just change value of targetDir\ne.g. targetDir=\"\" or targetDir=\"aSubDir/\"");
        
        response.append("strdef targetDir\n");
        response.append("sprint(targetDir, \"%s%s/\", simsDir, simReference)\n\n");

        response.append(generateMultiRunPreScript());

        response.append(generateRunMechanism());

        if (recordingSomething)
        {
            response.append("print \"Storing the data...\"\n\n");
            response.append("strdef timeFilename\n");

            String prefix = "";
            String post = "";

            if (simConfig.getMpiConf().isParallel())
            {
                prefix = "    ";
                post = "}" + "\n";
                response.append("if (hostid == 0) {\n");
            }


            response.append(prefix+"sprint(timeFilename, \"%s%s\", targetDir, \"" + SimulationData.TIME_DATA_FILE + "\")\n");
            response.append(prefix+"f_time.wopen(timeFilename)\n");
            response.append(prefix+"v_time.printf(f_time)\n");
            response.append(prefix+"f_time.close()\n");

            response.append(post);
            response.append("\n");


            for (PlotSaveDetails record : recordings)
            {
                String cellGroupName = record.simPlot.getCellGroup();

                int numInCellGroup = project.generatedCellPositions.getNumberInCellGroup(cellGroupName);
                String cellType = project.cellGroupsInfo.getCellType(cellGroupName);
                Cell cell = project.cellManager.getCell(cellType);

                boolean isSpikeRecording = record.simPlot.getValuePlotted().indexOf(SimPlot.SPIKE) >= 0;

                if (numInCellGroup > 0)
                {
                    addHocComment(response, record.getDescription(true));

                    for (Integer segId : record.segIdsToPlot)
                    {
                        Segment segToRecord = cell.getSegmentWithId(segId);

                        if (record.allCellsInGroup && !record.simPlot.isSynapticMechanism())
                        {

                            addHocComment(response, "Saving vector for segment: "
                                       + segToRecord.getSegmentName() + "(ID: " + segToRecord.getSegmentId() + ")");

                            String objName = this.getObjectName(record, -1, getHocSegmentName(segToRecord.getSegmentName()));

                            if (isSpikeRecording) objName = objName + "_spike";

                            String vectObj = "v_" + objName;
                            String fileObj = "f_" + objName;
                            
                            vectObj = GeneralUtils.replaceAllTokens(vectObj, ".", "_");
                            fileObj = GeneralUtils.replaceAllTokens(fileObj, ".", "_");

                            response.append("for i=0, " + (numInCellGroup - 1) + " {\n");

                            prefix = "";
                            post = "";

                            if (simConfig.getMpiConf().isParallel())
                            {
                                prefix = "    ";
                                post = "    }" + "\n";
                                response.append("    if (isCellOnNode(\""
                                                + cellGroupName + "\", i)) {\n");
                            }


                            response.append(prefix+"    " + fileObj + "[i] = new File()\n");
                            response.append(prefix+"    strdef filename\n");

                            String fileName = SimPlot.getFilename(record, segToRecord, "%d");

                            response.append(prefix+"    sprint(filename, \"%s" + fileName + "\", targetDir, i)\n");
                            response.append(prefix+"    " + fileObj + "[i].wopen(filename)\n");
                            response.append(prefix+"    " + vectObj + "[i].printf(" + fileObj + "[i])\n");
                            response.append(prefix+"    " + fileObj + "[i].close()\n");
                            response.append(post);
                            response.append("}\n\n");
                        }
                        else
                        {
                            for (Integer cellNum : record.cellNumsToPlot)
                            {
                                if (record.simPlot.isSynapticMechanism())
                                {
                                    String neuronVar = this.convertToNeuronVarName(record.simPlot.getValuePlotted());

                                    String netConn = SimPlot.getNetConnName(record.simPlot.getValuePlotted());
                                    String synType = SimPlot.getSynapseType(record.simPlot.getValuePlotted());

                                    /** @todo Make more efficient, as most synObjs for seg ids will be empty... */

                                    ArrayList<PostSynapticObject> synObjs
                                        = project.generatedNetworkConnections.getSynObjsPresent(netConn,
                                        synType,
                                        cellNum,
                                        segId);

                                    logger.logComment("Syn objs for: " + netConn + ", " + synType + ", cellNum: "
                                                      + cellNum + ", segId: " + segId + ": " + synObjs);

                                    for (PostSynapticObject postSynObj : synObjs)
                                    {
                                        String synObjName = this.getSynObjName(postSynObj);

                                        //String var = synObjName + "." + neuronVar;

                                        String vectorObj = "v_" + synObjName + "_" + neuronVar;
                                        String fileObj = "f_" + synObjName + "_" + neuronVar;
                                        
                                        vectorObj = GeneralUtils.replaceAllTokens(vectorObj, ".", "_");
                                        fileObj = GeneralUtils.replaceAllTokens(fileObj, ".", "_");

                                        prefix = "";
                                        post = "";

                                        if (simConfig.getMpiConf().isParallel())
                                        {
                                            prefix = "    ";
                                            post = "}" + "\n";
                                            response.append("if (isCellOnNode(\""
                                                            + cellGroupName + "\", " + cellNum + ")) {\n");
                                        }

                                        response.append(prefix + fileObj + " = new File()\n");
                                        response.append(prefix + "strdef filename\n");

                                        String fileName = SimPlot.getFilename(record, postSynObj, "%d");

                                        response.append(prefix + "sprint(filename, \"%s" + fileName + "\", targetDir, " +
                                                        cellNum + ")\n");

                                        response.append(prefix + fileObj + ".wopen(filename)\n");
                                        response.append(prefix + vectorObj + ".printf(" + fileObj + ")\n");
                                        response.append(prefix + fileObj + ".close()\n\n");
                                        response.append(post);

                                    }
                                }
                                else
                                {

                                    addHocComment(response,
                                               "Saving vector for segment: " + segToRecord.getSegmentName() +
                                               "(ID: " + segToRecord.getSegmentId() + ") in cell number: " +
                                               cellNum);

                                    String objName = this.getObjectName(record, cellNum, getHocSegmentName(segToRecord.getSegmentName()));

                                    if (isSpikeRecording) objName = objName + "_spike";

                                    String vectObj = "v_" + objName;
                                    String fileObj = "f_" + objName;
                                    
                             
                                vectObj = GeneralUtils.replaceAllTokens(vectObj, ".", "_");
                                fileObj = GeneralUtils.replaceAllTokens(fileObj, ".", "_");

                                    prefix = "";
                                    post = "";

                                    if (simConfig.getMpiConf().isParallel())
                                    {
                                        prefix = "    ";
                                        post = "}" + "\n";
                                        response.append("if (isCellOnNode(\""
                                                        + cellGroupName + "\", " + cellNum + ")) {\n");
                                    }

                                    response.append(prefix + fileObj + " = new File()\n");
                                    response.append(prefix + "strdef filename\n");

                                    String fileName = SimPlot.getFilename(record, segToRecord, "%d");

                                    response.append(prefix + "sprint(filename, \"%s" + fileName + "\", targetDir, " +
                                                    cellNum + ")\n");
                                    response.append(prefix + fileObj + ".wopen(filename)\n");
                                    response.append(prefix + vectObj + ".printf(" + fileObj + ")\n");
                                    response.append(prefix + fileObj + ".close()\n\n");
                                    response.append(post);
                                }
                            }
                        }
                    }
                }
            }
            response.append("\n");

            prefix = "";
            post = "";

            if (simConfig.getMpiConf().isParallel())
            {
                prefix = "    ";
                post = "}" + "\n";

                response.append("if (hostid == 0) {\n");
            }

            response.append(prefix+"propsFile = new File()\n");
            response.append(prefix+"strdef propsFilename\n");
            response.append(prefix+"sprint(propsFilename, \"%s" + SimulationsInfo.simulatorPropsFileName + "\", targetDir)\n");
            response.append(prefix+"propsFile.wopen(propsFilename)\n");
            response.append(prefix+
                "propsFile.printf(\"#This is a list of properties generated by NEURON during the simulation run\\n\")\n");



            if (this.savingHostname()) response.append(prefix+"propsFile.printf(\"Host=%s\\n\", host)\n");

            response.append(prefix+"propsFile.printf(\"RealSimulationTime=%g\\n\", realtime)\n");
            response.append(prefix+"propsFile.printf(\"SimulationSetupTime=%g\\n\", setuptime)\n");

            response.append(generateMultiRunPostScript());
            response.append(prefix+"propsFile.close()\n");
            response.append(post);
            response.append("\n");

            response.append(prefix+"print \"Data stored in directory: \", targetDir\n\n");
        }

        return response.toString();

    }
    
    private boolean savingHostname()
    {
        // There have been some problems getting C:/WINDOWS/SYSTEM32/hostname.exe to run on win
        // so temporarily disableing it. It's only needed for parallel running of sims, which is 
        // unlikely on win for the forseeable future.
        
        if (GeneralUtils.isWindowsBasedPlatform()) return false;
        
        return true;
    }

    private String getObjectName(PlotSaveDetails record, int cellNum, String segName)
    {

        String variable = "_" + this.convertToNeuronVarName(record.simPlot.getValuePlotted());


        if (cellNum<0)
        {
            return SimEnvHelper.getSimulatorFriendlyName(record.simPlot.getCellGroup() + "_seg_" + segName+ variable);
        }

        return SimEnvHelper.getSimulatorFriendlyName(record.simPlot.getCellGroup()+"_cn"+cellNum
                                                     + "_seg_" + segName+ variable);
    }



    public String generateCellGroups() throws NeuronException
    {
        StringBuffer response = new StringBuffer();
        //response.append("\n");

        ArrayList<String> cellGroupNames = project.cellGroupsInfo.getAllCellGroupNames();
        
        logger.logComment("Looking at " + cellGroupNames.size() + " cell groups");

        if (cellGroupNames.size() == 0)
        {
            logger.logComment("There are no cell groups!!");

            addMajorComment(response, "There were no cell groups specified in the project...");
            return response.toString();
        }


        GeneralUtils.timeCheck("Starting gen of cell groups");
        
        String prefix = "";
        if (isRunModePythonBased(genRunMode)) prefix = "h.";


        for (int ii = 0; ii < cellGroupNames.size(); ii++)
        {
            String cellGroupName = cellGroupNames.get(ii);

            logger.logComment("***  Looking at cell group number " + ii
                              + ", called: " + cellGroupName);

            if (project.generatedCellPositions.getNumberInCellGroup(cellGroupName) == 0)
            {
                logger.logComment("No cells generated in that group. Ignoring...");
            }
            else
            {

                String cellTypeName = project.cellGroupsInfo.getCellType(cellGroupName);

                addMajorComment(response, "Cell group "
                                       + ii
                                       + ": "
                                       + cellGroupName
                                       + " has cells of type: "
                                       + cellTypeName);

                Cell cell = project.cellManager.getCell(cellTypeName);

                // better create the hoc file for this...

                File dirForNeuronFiles = ProjectStructure.getNeuronCodeDir(project.getProjectMainDirectory());

                logger.logComment("Dir for NeuronFiles: " + dirForNeuronFiles);

                ArrayList<Object> cellMechanisms = new ArrayList<Object>();
                cellMechanisms.addAll(cell.getAllChannelMechanisms(true));

                //Vector allSyns = cell.getAllAllowedSynapseTypes();

                Iterator allNetConns = project.generatedNetworkConnections.getNamesNetConnsIter();

                while (allNetConns.hasNext())
                {
                    String netConnName = (String) allNetConns.next();

                    if (project.generatedNetworkConnections.getNumberSynapticConnections(GeneratedNetworkConnections.ANY_NETWORK_CONNECTION) > 0)
                    {
                        if (project.morphNetworkConnectionsInfo.getTargetCellGroup(netConnName) != null
                            && cellGroupName.equals(project.morphNetworkConnectionsInfo.getTargetCellGroup(netConnName)))
                        {
                            Vector synapses = project.morphNetworkConnectionsInfo.getSynapseList(netConnName);
                            for (int i = 0; i < synapses.size(); i++)
                            {
                                SynapticProperties next = (SynapticProperties) synapses.elementAt(i);
                                cellMechanisms.add(next.getSynapseType());
                            }

                        }
                        if (project.volBasedConnsInfo.getTargetCellGroup(netConnName) != null
                            && cellGroupName.equals(project.volBasedConnsInfo.getTargetCellGroup(netConnName)))
                        {
                            Vector synapses = project.volBasedConnsInfo.getSynapseList(netConnName);
                            for (int i = 0; i < synapses.size(); i++)
                            {
                                SynapticProperties next = (SynapticProperties) synapses.elementAt(i);
                                cellMechanisms.add(next.getSynapseType());
                            }

                        }

                    }
                }

                Vector allStims = project.elecInputInfo.getAllStims();

                logger.logComment("All stims: " + allStims);

                for (int stimNo = 0; stimNo < allStims.size(); stimNo++)
                {
                    StimulationSettings next = (StimulationSettings) allStims.elementAt(stimNo);
                    if (next.getCellGroup().equals(cellGroupName))
                    {
                        if (next instanceof RandomSpikeTrainSettings)
                        {
                            RandomSpikeTrainSettings spikeSettings = (RandomSpikeTrainSettings) next;
                            if (!cellMechanisms.contains(spikeSettings.getSynapseType()))
                            {
                                cellMechanisms.add(spikeSettings.getSynapseType());
                            }
                        }
                        else if (next instanceof RandomSpikeTrainExtSettings)
                        {
                            RandomSpikeTrainExtSettings spikeSettings = (RandomSpikeTrainExtSettings) next;
                            if (!cellMechanisms.contains(spikeSettings.getSynapseType()))
                            {
                                cellMechanisms.add(spikeSettings.getSynapseType());
                            }
                        }


                    }
                }

                logger.logComment("------------    All cell mechs for "+cellGroupName+": " + cellMechanisms);

                for (int i = 0; i < cellMechanisms.size(); i++)
                {

                    CellMechanism cellMechanism = null;

                    if (cellMechanisms.get(i) instanceof String)
                        cellMechanism = project.cellMechanismInfo.getCellMechanism( (String) cellMechanisms.get(i));

                    else if (cellMechanisms.get(i) instanceof ChannelMechanism)
                    {
                        logger.logComment("Is a ChannelMechanism...");
                        ChannelMechanism nextCellMech = (ChannelMechanism) cellMechanisms.get(i);
                        cellMechanism = project.cellMechanismInfo.getCellMechanism(nextCellMech.getName());
                    }

                    if (cellMechanism == null)
                    {
                        throw new NeuronException("Problem generating file for cell mech: " + cellMechanisms.get(i)
                            + "\nPlease ensure there is an implementation for that mechanism in NEURON");
                        
                    }

                    logger.logComment("Looking at cell mechanism: " + cellMechanism.getInstanceName());

                    if (!testForInbuiltModFile(cellMechanism.getInstanceName(), dirForNeuronFiles))
                    {
                        if (!cellMechFilesGenAndIncl.contains(cellMechanism.getInstanceName()))
                        {
                            logger.logComment("Cell mechanism: " + cellMechanism.getInstanceName()+" was not handled already");
                            boolean success = true;
                            boolean regenerate = project.neuronSettings.isForceModFileRegeneration();
                            
                            File sourceFilesDir = new File(ProjectStructure.getCellMechanismDir(project.getProjectMainDirectory()),cellMechanism.getInstanceName());
                            
                            File[] sourceFiles = sourceFilesDir.listFiles();
                            File[] targetFiles = dirForNeuronFiles.listFiles();
                            
                            if (targetFiles.length == 0)
                                regenerate = true;
                            
                            if (!regenerate)
                            {
                                for(File sourceFile: sourceFiles)
                                {
                                    boolean foundTarget = false;
                                    
                                    for(File targetFile: targetFiles)
                                    {
                                        if (targetFile.getName().indexOf(".")>0)
                                        {
                                            String targetPossMechName = targetFile.getName().substring(0, targetFile.getName().indexOf("."));

                                            if (targetPossMechName.equals(cellMechanism.getInstanceName()))
                                            {
                                                foundTarget = true;
                                                boolean sourceNewer = sourceFile.lastModified() > targetFile.lastModified();
                                                logger.logComment("Is "+sourceFile+" newer than "+ targetFile+"? "+sourceNewer );
                                                if (sourceNewer) 
                                                    regenerate = true;
                                            }
                                        }
                                    }
                                    if (!foundTarget)
                                        regenerate = true;
                                }
                            }
                            if (regenerate || !firstRecompileComplete)
                            {
                                firstRecompileComplete = true;
                                logger.logComment("Regenerating..." );
                                if (cellMechanism instanceof AbstractedCellMechanism)
                                {
                                    File newMechFile = new File(dirForNeuronFiles,
                                                                cellMechanism.getInstanceName() + ".mod");

                                    success = ( (AbstractedCellMechanism) cellMechanism).createImplementationFile(SimEnvHelper.
                                        NEURON,
                                        UnitConverter.NEURON_UNITS,
                                        newMechFile,
                                        project,
                                        true,
                                        addComments,
                                        project.neuronSettings.isForceCorrectInit());
                                }
                                else if (cellMechanism instanceof ChannelMLCellMechanism)
                                {
                                    ChannelMLCellMechanism cmlMechanism = (ChannelMLCellMechanism) cellMechanism;
                                    File newMechFile = null;

                                    logger.logComment("Sim map: " + cmlMechanism.getSimMapping(SimEnvHelper.NEURON));

                                    if (cmlMechanism.getSimMapping(SimEnvHelper.NEURON).isRequiresCompilation())
                                    {
                                        newMechFile = new File(dirForNeuronFiles,
                                                               cellMechanism.getInstanceName() + ".mod");
                                    }
                                    else
                                    {
                                        newMechFile = new File(dirForNeuronFiles,
                                                               cellMechanism.getInstanceName() + ".hoc");

                                        response.append("load_file(\"" + cellMechanism.getInstanceName() + ".hoc\")\n");

                                    }
                                    success = cmlMechanism.createImplementationFile(SimEnvHelper.
                                        NEURON,
                                        UnitConverter.NEURON_UNITS,
                                        newMechFile,
                                        project,
                                        cmlMechanism.getSimMapping(SimEnvHelper.NEURON).isRequiresCompilation(),
                                        addComments,
                                        project.neuronSettings.isForceCorrectInit());
                                }
                            }

                            if (!success)
                            {
                                throw new NeuronException("Problem generating file for cell mechanism: " + cellMechanisms.get(i)
                                                          +"\nPlease ensure there is an implementation for that mechanism in NEURON");

                            }

                            cellMechFilesGenAndIncl.add(cellMechanism.getInstanceName());
                        }
                    }

                }

                logger.logComment("------    needsGrowthFunctionality: " + needsGrowthFunctionality(cellGroupName));
                
                boolean addSegIdFunctions = false;
                if (isRunModePythonBased(genRunMode)) addSegIdFunctions = true;

                NeuronTemplateGenerator cellTemplateGen
                    = new NeuronTemplateGenerator(project,
                                                  cell,
                                                  dirForNeuronFiles,
                                                  needsGrowthFunctionality(cellGroupName),
                                                  addSegIdFunctions);

                String filenameToBeGenerated = cellTemplateGen.getHocFilename();

                logger.logComment("Will need a cell template file called: " +
                                  filenameToBeGenerated);

                if (cellTemplatesGenAndIncluded.contains(filenameToBeGenerated))
                {
                    addComment(response, "Cell template file: "+cellTemplateGen.getHocShortFilename()
                            +" for cell group "+cellGroupName+" has already been included");
                }
                else
                {
                    logger.logComment("Generating it...");
                    try
                    {
                        cellTemplateGen.generateFile();

                        cellTemplatesGenAndIncluded.add(filenameToBeGenerated);
                        
                        logger.logComment("Adding include for the file to the main hoc file...");

                        StringBuffer fileNameBuffer = new StringBuffer(filenameToBeGenerated);

                        for (int j = 0; j < fileNameBuffer.length(); j++)
                        {
                            char c = fileNameBuffer.charAt(j);
                            if (c == '\\')
                                fileNameBuffer.replace(j, j + 1, "/");
                        }

                        addComment(response, "Adding cell template file: "+cellTemplateGen.getHocShortFilename()
                                +" for cell group "+cellGroupName+"");
                        
                        response.append(prefix+"load_file(\"" + cellTemplateGen.hocFile.getName() + "\")\n");
                    }
                    catch (NeuronException ex)
                    {
                        logger.logError("Problem generating one of the template files...", ex);
                        throw ex;
                    }
                }

                
                
                // now we've got the includes
                
                if (!isRunModePythonBased(genRunMode))
                {
                    String currentRegionName = project.cellGroupsInfo.getRegionName(cellGroupName);
    
                    ArrayList cellGroupPositions = project.generatedCellPositions.getPositionRecords(cellGroupName);
    
                    addHocComment(response, "Adding " + cellGroupPositions.size()
                                      + " cells of type " + cellTypeName
                                      + " in region " + currentRegionName);
    
                    String nameOfNumberOfTheseCells = "n_" + cellGroupName;
                    String nameOfArrayOfTheseCells = "a_" + cellGroupName;
    
                    if (cellGroupPositions.size() > 0)
                    {
                        response.append(nameOfNumberOfTheseCells + " = " + cellGroupPositions.size() + "\n\n");
    
                        response.append("objectvar " + nameOfArrayOfTheseCells + "[" + nameOfNumberOfTheseCells + "]" +
                                        "\n\n");
    
                        if (!simConfig.getMpiConf().isParallel())
                        {
                            response.append("proc addCell_" + cellGroupName + "() {\n");
    
                            response.append("    strdef reference\n");
                            response.append("    sprint(reference, \"" + cellGroupName + "_%d\", $1)\n");
    
                            String desc = GeneralUtils.replaceAllTokens(cell.getCellDescription(), "\n", " ");
                            response.append("    " + nameOfArrayOfTheseCells + "[$1] = new " + cellTypeName +
                                            "(reference, \""
                                            + cellTypeName + "\", \"" + desc + "\")" + "\n");
    
                            response.append("    allCells.append(" + nameOfArrayOfTheseCells + "[$1])\n");
    
                            response.append("}" + "\n\n");
    
                            response.append("for i = 0, " + nameOfNumberOfTheseCells + "-1 {" + "\n");
    
                            response.append("    addCell_" + cellGroupName + "(i)" + "\n\n");
    
                            response.append("}" + "\n\n");
                        }
                        else
                        {
    
                            response.append("for i = 0, " + nameOfNumberOfTheseCells + "-1 {" + "\n");
    
                            //response.append("addCell_" + cellGroupName + "(i)" + "\n\n");
    
                            response.append("    if(isCellOnNode(\""+cellGroupName+"\", i)) {\n");
    
                            response.append("        strdef reference, type, description\n");
                            response.append("        sprint(reference, \"" + cellGroupName + "_%d\", i)\n");
                            response.append("        sprint(type, \"" + cellTypeName + "\")\n");
                            response.append("        sprint(description, \"" + GeneralUtils.replaceAllTokens(cell.getCellDescription(), "\n", " ") + "\")\n");
    
                            //response.append("        strdef command\n");
                            //response.append("        sprint(command, \"new " + cellTypeName + "(reference, type, description)\")\n");
    
                            if (addComments) response.append("        print \"Going to create cell: \", reference, \" on host \", host, \", id: \", hostid\n");
    
                            //response.append("    pnm.create_cell(i, command)\n");
                            response.append( "        a_"+cellGroupName+"[i] = new "+cellTypeName+"(reference, type, description)\n");
    
    
                            response.append("        pnm.register_cell(getCellGlobalId(\""+cellGroupName+"\", i), a_"+cellGroupName+"[i])\n");

                            response.append("        allCells.append(" + nameOfArrayOfTheseCells + "[i])\n");
    
                            response.append("    }\n");
    
                            response.append("}" + "\n\n");
                        }
                    }
    
    
    
                    Region regionInfo = project.regionsInfo.getRegionObject(currentRegionName);
                    CellPackingAdapter packer = project.cellGroupsInfo.getCellPackingAdapter(cellGroupName);
    
                    //     float yDisplacementOfThisRegion = project.regionsInfo.getYDisplacementOfRegion(currentRegionName);
    
                    addHocComment(response, "Placing these cells in a region described by: " + regionInfo);
                    addHocComment(response, "Packing has been generated by: " + packer.toString());
    
                    for (int j = 0; j < cellGroupPositions.size(); j++)
                    {
                        PositionRecord posRecord
                            = (PositionRecord) cellGroupPositions.get(j);
    
                        logger.logComment("Moving cell number: " + j + " into place");
    
                        if (j != posRecord.cellNumber)
                        {
                            // not really a problem, but best to highlight it...
                            logger.logComment("-------------------------                Position number " + j +
                                              " doesn't match cell number: " + posRecord);
                            // continue...
                        }
    
                        String parallelCheck = "";
                        if (simConfig.getMpiConf().isParallel())
                            parallelCheck = "if (isCellOnNode(\""+cellGroupName+"\", "+posRecord.cellNumber+")) ";
    
                        response.append(parallelCheck+nameOfArrayOfTheseCells + "[" + posRecord.cellNumber + "].position("
                                        + posRecord.x_pos + "," + posRecord.y_pos + "," + posRecord.z_pos + ")\n");
    
                    }
                }


                logger.logComment("***  Finished looking at cell group number " + ii + ", called: " + cellGroupName);

            }

            response.append("\n");

        }
        
        boolean genAllModFiles = project.neuronSettings.isGenAllModFiles(); 
        
        
        if (genAllModFiles)
        {
            
                File dirForNeuronFiles = ProjectStructure.getNeuronCodeDir(project.getProjectMainDirectory());
        	Vector<String> allAvailableMods = project.cellMechanismInfo.getAllCellMechanismNames();
        	
        	for(String cellMech:  allAvailableMods)
        	{
                    if (!testForInbuiltModFile(cellMech, dirForNeuronFiles))
                    {
                        if (!cellMechFilesGenAndIncl.contains(cellMech))
                        {
                            CellMechanism cellMechanism = project.cellMechanismInfo.getCellMechanism(cellMech);

                            boolean success = false;
                            if (cellMechanism instanceof AbstractedCellMechanism)
                            {
                                File newMechFile = new File(dirForNeuronFiles,
                                                            cellMechanism.getInstanceName() + ".mod");

                                success = ( (AbstractedCellMechanism) cellMechanism).createImplementationFile(SimEnvHelper.
                                    NEURON,
                                    UnitConverter.NEURON_UNITS,
                                    newMechFile,
                                    project,
                                    true,
                                    addComments,
                                    project.neuronSettings.isForceCorrectInit());
                            }
                            else if (cellMechanism instanceof ChannelMLCellMechanism)
                            {
                                ChannelMLCellMechanism cmlMechanism = (ChannelMLCellMechanism) cellMechanism;
                                File newMechFile = null;

                                logger.logComment("Sim map: " + cmlMechanism.getSimMapping(SimEnvHelper.NEURON));

                                if (cmlMechanism.getSimMapping(SimEnvHelper.NEURON).isRequiresCompilation())
                                {
                                    newMechFile = new File(dirForNeuronFiles,
                                                           cellMechanism.getInstanceName() + ".mod");
                                }
                                else
                                {
                                    newMechFile = new File(dirForNeuronFiles,
                                                           cellMechanism.getInstanceName() + ".hoc");

                                    response.append("load_file(\"" + cellMechanism.getInstanceName() + ".hoc\")\n");

                                }
                                success = cmlMechanism.createImplementationFile(SimEnvHelper.
                                    NEURON,
                                    UnitConverter.NEURON_UNITS,
                                    newMechFile,
                                    project,
                                    cmlMechanism.getSimMapping(SimEnvHelper.NEURON).isRequiresCompilation(),
                                    addComments,
                                    project.neuronSettings.isForceCorrectInit());
                            }

                            if (!success)
                            {
                                throw new NeuronException("Problem generating file for cell mechanism: "
                                                          + cellMechanism
                                                          +
                                                          "\nPlease ensure there is an implementation for that mechanism in NEURON");

                            }

                            cellMechFilesGenAndIncl.add(cellMechanism.getInstanceName());
                        }
                    }
        	}
        }

        GeneralUtils.timeCheck("Finished gen of cell groups");

        return response.toString();

    }
    
    private boolean testForInbuiltModFile(String mechName, File targetDir)
    {
        ArrayList<String> inbuiltMechs = new ArrayList<String>();
        
        //@todo There are more to add...
        
        inbuiltMechs.add("pas");
        inbuiltMechs.add("hh");
        inbuiltMechs.add("extracellular");
        inbuiltMechs.add("fastpas");
        
        if (inbuiltMechs.contains(mechName))
        {
            File warnFile = new File(targetDir, mechName+".mod.WARNING");
            
            if (!warnFile.exists())
            {
                try
                {
                    FileWriter fwReadme = new FileWriter(warnFile);
                    fwReadme.write("Warning: there is a mechanism "+mechName+" in the neuroConstruct project, which uses the same name as\n"
                        +"a built in mechanism in NEURON. This is possibly due to export of a Level 2 morphology from NEURON.\n"+
                        "Using the NEURON mechanism instead of that from neuroConstruct!! Hopefully the functionality is the same!!");
                    fwReadme.close();
                }
                catch (IOException ex)
                {
                    logger.logError("Exception creating "+warnFile+"...");
                }
            }
            return true;
        }
        return false;
    }

    private boolean needsGrowthFunctionality(String cellGroup)
    {
        Vector allNetConnNames = project.morphNetworkConnectionsInfo.getAllSimpleNetConnNames();

        for (int i = 0; i < allNetConnNames.size(); i++)
        {
            String netConnName = (String) allNetConnNames.elementAt(i);
            logger.logComment("Checking: " + netConnName + " for growth func for cell group : " + cellGroup);
            if (project.morphNetworkConnectionsInfo.getSourceCellGroup(netConnName).equals(cellGroup))
            {
                logger.logComment("The cellGroup is source");
                if (project.morphNetworkConnectionsInfo.getGrowMode(netConnName).getType()
                    != GrowMode.GROW_MODE_JUMP)
                    return true;
            }
            if (project.morphNetworkConnectionsInfo.getTargetCellGroup(netConnName).equals(cellGroup))
            {
                logger.logComment("The cellGroup is target");
                if (project.morphNetworkConnectionsInfo.getGrowMode(netConnName).getType()
                    != GrowMode.GROW_MODE_JUMP)
                    return true;
            }
        }
        return false;

    }



    private String getSynObjName(PostSynapticObject synDetails)
    {
        String objectVarName = "syn_" + synDetails.getNetConnName()
            + "_" + synDetails.getSynapseType()
            + "_" + synDetails.getSynapseIndex();

        return objectVarName;

    }

    private String generateNetworkConnections()
    {

        logger.logComment("Starting generation of the net conns");
        
        int totNetConns = project.generatedNetworkConnections.getNumberSynapticConnections(GeneratedNetworkConnections.ANY_NETWORK_CONNECTION);

        StringBuffer response = new StringBuffer(totNetConns*800); // initial capacity...

        response.append("\n");

        addMajorHocComment(response, "Adding Network Connections");
        
        if (simConfig.getMpiConf().isParallel())
        {
            response.append("objectvar allCurrentNetConns\n");
            response.append("allCurrentNetConns = new List()\n\n");
        }
        
        if (addComments && simConfig.getMpiConf().isParallel())
        {
            //response.append("for i=0, (20000000 *hostid)  { rr = i*i }\n"); 
            response.append("print \" -----------------------   Starting generation of net conns on host: \", hostid\n\n"); 

        }
        
        Hashtable<String, Integer> preSectionsVsGids = new Hashtable<String, Integer>();
        

        //response.append("objectvar nil\n\n");

        Iterator allNetConnNames = project.generatedNetworkConnections.getNamesNetConnsIter();

        if (!allNetConnNames.hasNext())
        {
            logger.logComment("There are no synaptic connections");
            return "";
        }

        GeneralUtils.timeCheck("Starting gen of syn conns");


        // refresh iterator...
        allNetConnNames = project.generatedNetworkConnections.getNamesNetConnsIter();

        int globalPreSynId = 10000000;
        
        // Adding specific network connections...
        
        while (allNetConnNames.hasNext())
        {
            String netConnName = (String) allNetConnNames.next();

            GeneralUtils.timeCheck("Generating net conn: "+ netConnName);

            String sourceCellGroup = null;
            String targetCellGroup = null;
            Vector<SynapticProperties> synPropList = null;

            if (project.morphNetworkConnectionsInfo.isValidSimpleNetConn(netConnName))
            {
                sourceCellGroup = project.morphNetworkConnectionsInfo.getSourceCellGroup(netConnName);
                targetCellGroup = project.morphNetworkConnectionsInfo.getTargetCellGroup(netConnName);
                synPropList = project.morphNetworkConnectionsInfo.getSynapseList(netConnName);
            }

            else if (project.volBasedConnsInfo.isValidAAConn(netConnName))
            {
                sourceCellGroup = project.volBasedConnsInfo.getSourceCellGroup(netConnName);
                targetCellGroup = project.volBasedConnsInfo.getTargetCellGroup(netConnName);
                synPropList = project.volBasedConnsInfo.getSynapseList(netConnName);
            }

            String targetCellName = project.cellGroupsInfo.getCellType(targetCellGroup);
            Cell targetCell = project.cellManager.getCell(targetCellName);

            String sourceCellName = project.cellGroupsInfo.getCellType(sourceCellGroup);
            Cell sourceCell = project.cellManager.getCell(sourceCellName);

            Hashtable<Integer, SegmentLocation> substituteConnPoints
                = new Hashtable<Integer, SegmentLocation> (); // used for storing alternate connection locations
            // when ApPropSpeed on sections..

            if (sourceCell.getApPropSpeedsVsGroups().size() > 0) // are there any?
            {
                ArrayList<Section> allSecs = sourceCell.getAllSections();

                for (int j = 0; j < allSecs.size(); j++)
                {
                    Section nextSec = allSecs.get(j);

                    if (sourceCell.getApPropSpeedForSection(nextSec) != null)
                    {
                        LinkedList<Segment> segs = sourceCell.getAllSegmentsInSection(nextSec);

                        SegmentLocation synconloc = CellTopologyHelper.getConnLocOnExpModParent(sourceCell,
                            segs.getFirst());
                        //Segment subsSeg = sourceCell.getSegmentWithId(synconloc.segmentId);

                        for (int k = 0; k < segs.size(); k++)
                        {
                            int id = segs.get(k).getSegmentId();
                            substituteConnPoints.put(new Integer(id), synconloc);
                        }
                    }
                }
            }
            
            boolean isGapJunction = false;
            
            if (synPropList.size()==1)
            {
                CellMechanism cm = project.cellMechanismInfo.getCellMechanism(synPropList.get(0).getSynapseType());
                if (cm.getMechanismType().equals(CellMechanism.GAP_JUNCTION))
                    isGapJunction = true;
            }

            ArrayList<SingleSynapticConnection> allSynapses = project.generatedNetworkConnections.getSynapticConnections(netConnName);
            
            response.append("\n");
            addHocComment(response, "Adding NetConn: "
                              + netConnName
                              + " from: "
                              + sourceCellGroup
                              + " to: "
                              + targetCellGroup+" with "+allSynapses.size()+" connections\neach with syn(s): "+synPropList);

            response.append("\n");
            
            if (simConfig.getMpiConf().isParallel())
            {
                //////////response.append("allCurrentNetConns.remove_all()   // Empty list \n");
            }


            GeneralUtils.timeCheck("Have all info for net conn: "+ netConnName);

            for (int singleConnIndex = 0; singleConnIndex < allSynapses.size(); singleConnIndex++)
            {
                GeneratedNetworkConnections.SingleSynapticConnection synConn = allSynapses.get(singleConnIndex);
                //System.out.println("synConn: "+synConn);

                for (int synPropIndex = 0; synPropIndex < synPropList.size(); synPropIndex++)
                {
                    SynapticProperties synProps = synPropList.elementAt(synPropIndex);

                    PostSynapticObject synObj = new PostSynapticObject(netConnName,
                                                         synProps.getSynapseType(),
                                                         synConn.targetEndPoint.cellNumber,
                                                         synConn.targetEndPoint.location.getSegmentId(),
                                                         singleConnIndex);


                    String objectVarName = getSynObjName(synObj);
                    if (isGapJunction) objectVarName = "elec"+objectVarName;

                    /** @todo Remove the need for this... Revise how inbuilt synapses are stored/checked.. */

                    String synapseType = null;
                    if (synProps.getSynapseType().indexOf(" ") > 0)
                    {
                        synapseType = synProps.getSynapseType().substring(0, synProps.getSynapseType().indexOf(" "));
                    }
                    else
                    {
                        synapseType = synProps.getSynapseType();
                    }

                    double threshold = synProps.getThreshold();

                    //NumberGenerator delayGenerator = synProps.delayGenerator;
                    //NumberGenerator weightsGenerator = synProps.weightsGenerator;

                    Segment targetSegment
                        = targetCell.getSegmentWithId(synConn.targetEndPoint.location.getSegmentId());

                    logger.logComment("Target segment: " + targetSegment);

                    float fractionAlongTargetSection
                        = CellTopologyHelper.getFractionAlongSection(targetCell,
                                                                     targetSegment,
                                                                     synConn.targetEndPoint.location.getFractAlong());

                    Segment sourceSegment = null;
                    float fractionAlongSrcSeg = -1;
                    
                    int origId = synConn.sourceEndPoint.location.getSegmentId();

                    float apSegmentPropDelay = 0;

                    if (substituteConnPoints.size() == 0 || // there is no ApPropSpeed on cell
                        !substituteConnPoints.containsKey(origId)) // none on this segment
                    {
                        sourceSegment = sourceCell.getSegmentWithId(origId);
                        fractionAlongSrcSeg = synConn.sourceEndPoint.location.getFractAlong();
                        ///if (sourceSegment.isSpherical()) fractionAlongSrcSeg = 1; // as it doesn't really matter
                    }
                    else
                    {
                        Segment realSource = sourceCell.getSegmentWithId(origId);

                        SegmentLocation subsSynConLoc = substituteConnPoints.get(new Integer(origId));

                        sourceSegment = sourceCell.getSegmentWithId(subsSynConLoc.getSegmentId());
                        fractionAlongSrcSeg = subsSynConLoc.getFractAlong();

                        apSegmentPropDelay = CellTopologyHelper.getTimeToFirstExpModParent(sourceCell,
                                                                                    realSource,
                                                                                    synConn.sourceEndPoint.location.getFractAlong());

                        addHocComment(response,
                                   "Instead of point " + synConn.sourceEndPoint.location.getFractAlong() + " along seg: "
                                   + realSource.toShortString() + " connecting to point " +
                                   fractionAlongSrcSeg + " along seg: "
                                   + sourceSegment.toShortString() + "");

                    }

                    logger.logComment("source segment: " + sourceSegment);

                    float fractAlongSourceSection
                        = CellTopologyHelper.getFractionAlongSection(sourceCell,
                                                                     sourceSegment,
                                                                     fractionAlongSrcSeg);
                    

                    logger.logComment("fractAlongSourceSection: " + fractAlongSourceSection);

                    float synInternalDelay = -1;
                    float weight = -1;
                    
                    if (synConn.props==null || synConn.props.size()==0)
                    {
                        logger.logComment("Generating weight from: "+ synProps.getWeightsGenerator());
                        
                        synInternalDelay = synProps.getDelayGenerator().getNominalNumber();
                        weight = synProps.getWeightsGenerator().getNominalNumber();
                    }
                    else
                    {
                        logger.logComment("Generating weight from: "+ synConn.props);
                        
                        boolean found = false;
                        
                        for (ConnSpecificProps prop:synConn.props)
                        {
                            if (prop.synapseType.equals(synProps.getSynapseType()))
                            {
                                found = true;
                                synInternalDelay = prop.internalDelay;
                                weight = prop.weight;
                            }
                        }
                        if (!found)
                        {
                            logger.logComment("Generating weight from: "+ synProps.getWeightsGenerator());

                            synInternalDelay = synProps.getDelayGenerator().getNominalNumber();
                            weight = synProps.getWeightsGenerator().getNominalNumber();
                        }
                    }

                    String tgtCellName = "a_" + targetCellGroup + "[" + synConn.targetEndPoint.cellNumber + "]";
                    String tgtSecName = getHocSectionName(targetSegment.getSection().getSectionName());
                    String tgtSecNameFull = tgtCellName+ "." + tgtSecName;

                    String srcCellName = "a_" + sourceCellGroup + "[" + synConn.sourceEndPoint.cellNumber + "]";
                    String srcSecName = getHocSectionName(sourceSegment.getSection().getSectionName());
                    String srcSecNameFull = srcCellName + "."+ srcSecName;

                    float apSpaceDelay = synConn.apPropDelay;

                    float totalDelay = synInternalDelay + apSegmentPropDelay + apSpaceDelay;
                    
                    addHocComment(response, "Syn conn (type: "+synProps.getSynapseType()+") "
                            +"from "+srcSecName+" on src cell "+synConn.sourceEndPoint.cellNumber
                            +" to "+tgtSecName+" on tgt cell "+synConn.targetEndPoint.cellNumber
                            +"\nFraction along src section: "
                            + fractAlongSourceSection+", weight: " + weight
                            + "\nDelay due to AP prop along segs: " + apSegmentPropDelay
                            + ", delay due to AP jump pre -> post 3D location "+ apSpaceDelay
                            + "\nInternal synapse delay (from Synaptic Props): " + synInternalDelay
                            +", TOTAL delay: "+totalDelay);



                    
                    if (!isGapJunction)
                    {

                        response.append("objectvar " + objectVarName + "\n\n");
                        
                        if (!simConfig.getMpiConf().isParallel())
                        {
                            // put synaptic start point on source axon
                            response.append(tgtSecNameFull + " " + objectVarName
                                            + " = new " + synapseType + "(" + fractionAlongTargetSection + ")\n");

                            response.append(srcSecNameFull + " "  + tgtCellName
                                            + ".synlist.append(new NetCon(&v("+ fractAlongSourceSection + "), "
                                            + objectVarName+", "+threshold+", "+totalDelay+", "+weight + "))"+"\n\n");

                            CellMechanism cm = project.cellMechanismInfo.getCellMechanism(synProps.getSynapseType());

                            if (cm instanceof AbstractedCellMechanism)
                            {
                                AbstractedCellMechanism acm = (AbstractedCellMechanism)cm;

                                try
                                {
                                    if (acm.getParameter("RequiresXYZ") == 1)
                                    {
                                        Point3f synRelToCell = CellTopologyHelper.convertSegmentDisplacement(targetCell,
                                            targetSegment.getSegmentId(),
                                            synConn.targetEndPoint.location.getFractAlong());

                                        Point3f posAbsSyn  = project.generatedCellPositions.getOneCellPosition(targetCellGroup,
                                            synConn.targetEndPoint.cellNumber);

                                        posAbsSyn.add(synRelToCell);

                                        addHocComment(response, "Synapse location on cell: " + synRelToCell);
                                        addHocComment(response, "Synapse absolute location: " + posAbsSyn);

                                        response.append(objectVarName+".x = "+posAbsSyn.x+"\n");
                                        response.append(objectVarName+".y = "+posAbsSyn.y+"\n");
                                        response.append(objectVarName+".z = "+posAbsSyn.z+"\n\n");
                                    }
                                }
                                catch (CellMechanismException ex)
                                {
                                    logger.logComment("No xyz parameter: "+ex);
                                }
                            }
                        }
                        else
                        {
                            response.append("localSynapseId = -2\n");
                            response.append("globalPreSynId = "+globalPreSynId+" // provisional gid for NetCon\n");

                            String netConRef = "NetCon_"+globalPreSynId;

                            response.append("objectvar " + netConRef + "\n\n");
                            String ncTemp = netConRef + "_temp";
                            response.append("objectvar " + ncTemp+"\n\n");

                            String targetExists = "isCellOnNode(\""+ targetCellGroup + "\", "+ synConn.targetEndPoint.cellNumber + ")";
                            String sourceExists = "isCellOnNode(\""+ sourceCellGroup + "\", " + synConn.sourceEndPoint.cellNumber + ")";

                            
                            
                                    // Post synaptic setup
                            
                            
                            if (addComments) response.append("print \"> Doing post syn setup for "+netConRef+", "+srcCellName+" -> "+tgtCellName+"\"\n\n");

                            response.append("if ("+targetExists+") {\n");
                            if (addComments) response.append("    print \"Target IS on host: \", hostid\n\n");

                            response.append("    "+tgtSecNameFull+" " + objectVarName
                                            + " = new " + synapseType + "(" + fractionAlongTargetSection + ")\n");


                            response.append("    "+tgtCellName+".synlist.append( "+ objectVarName + " )\n");

                            response.append("    localSynapseId = "+tgtCellName+".synlist.count()-1\n");

                            if (addComments) response.append("    print \"Created: \", "+objectVarName+",\" on "+tgtCellName+" on host \", hostid\n\n");

                            response.append("} else {\n");
                            if (addComments) response.append("    print \"Target NOT on host: \", hostid\n\n");
                            response.append("}\n");


                            
                            
                            
                                    // Pre synaptic setup
                            
                            if (addComments) response.append("\nprint \"Doing pre syn setup for "+netConRef+"\"\n\n");
                            
                            response.append("if ("+sourceExists+") {\n");
                            
                            if (addComments) response.append("    print \"Source IS on host: \", hostid\n\n");
                            
                            int gidOfSource = globalPreSynId;

                            if (!preSectionsVsGids.containsKey(srcSecNameFull))
                            {
                                
                                if (addComments) response.append("    print \"No NetCon exists yet for section: "+srcSecNameFull+" on host \", hostid\n\n");
                                
                                response.append("    pnm.pc.set_gid2node(globalPreSynId, hostid)\n");

                                response.append("    "+srcSecNameFull+" "+netConRef+" = new NetCon(&v("+synConn.sourceEndPoint.location.getFractAlong()+"), nil)\n");
                                
                                response.append("    "+netConRef+".delay = "+(synInternalDelay + apSegmentPropDelay + apSpaceDelay)+"\n");
                                response.append("    "+netConRef+".weight = "+weight+" // not really needed on the pre side\n");
                                response.append("    "+netConRef+".threshold = "+synProps.getThreshold()+"\n\n");

                                response.append("    pnm.pc.cell(globalPreSynId, "+netConRef+")\n");

                                response.append("    allCurrentNetConns.append("+netConRef+")\n");

                                if (addComments) response.append("    print \"Created: \", "+netConRef+",\" on "+srcSecNameFull+" on host \", hostid\n\n");

                                preSectionsVsGids.put(srcSecNameFull, globalPreSynId);
                            }
                            else
                            {
                                gidOfSource = preSectionsVsGids.get(srcSecNameFull);
                                if (addComments) response.append("    print \"NetCon for "+srcSecNameFull+" on host \", hostid, \" was already created with gid "
                                        +gidOfSource+"\"\n\n");
                            }

                            response.append("} else {\n");
                            if (addComments) response.append("    print \"Source NOT on host: \", hostid\n\n");
                            response.append("}\n");
                            

                            if (addComments) response.append("netConInfoParallel("+netConRef+")\n\n");
                            
                            
                                    // Connecting post to pre
                            

                            if (addComments) response.append("\nprint \"Doing pre to post attach for "+netConRef+"\"\n\n");

                            response.append("if ("+targetExists+") {\n");
                            if (addComments) response.append("    print \"Target IS on host: \", hostid, \" using gid: \", globalPreSynId\n\n");

                            response.append("    gidOfSource = "+gidOfSource+"\n\n");

                            response.append("    "+ncTemp+" = pnm.pc.gid_connect(gidOfSource, a_" + targetCellGroup
                                            + "[" + synConn.targetEndPoint.cellNumber + "]"
                                            + ".synlist.object(localSynapseId))\n");

                            response.append("    "+ncTemp+".delay = "+(synInternalDelay + apSegmentPropDelay + apSpaceDelay)+"\n");
                            response.append("    "+ncTemp+".weight = "+weight+"\n");
                            response.append("    "+ncTemp+".threshold = "+synProps.getThreshold()+"\n\n");

                            if (addComments) response.append("    netConInfoParallel("+ncTemp+")\n\n");
                            response.append("} else {\n");
                            if (addComments) response.append("    print \"Target NOT on host: \", hostid\n\n");
                            response.append("}\n");

                            if (addComments) response.append("print \"< Done setup for "+netConRef+"\"\n\n");


                        }
                        globalPreSynId++;
                    }                       // if (!isGapJunction)
                    else
                    {
                        String gjListenA = objectVarName + "_A";
                        String gjListenB = objectVarName + "_B";
                        
                        response.append("objectvar " + gjListenA+"\n\n");
                        response.append("objectvar " + gjListenB+"\n\n");
                        
                        
                        response.append(tgtSecNameFull + " { "
                                        + gjListenA + " = new "
                                        + synapseType + "(" + fractionAlongTargetSection + ") }\n");
                        
                        response.append(gjListenA + ".weight = "+weight+"\n");

                        response.append("setpointer "+ gjListenA  + ".vgap, "
                                        + srcSecNameFull + ".v("+ fractAlongSourceSection + ")" + "\n\n");
                        
                        
                        response.append(srcSecNameFull + " { "
                                        + gjListenB + " = new "
                                        + synapseType + "(" + fractAlongSourceSection + ") }\n");

                        response.append(gjListenB + ".weight = "+weight+"\n");
                        
                        response.append("setpointer "+ gjListenB  + ".vgap, "
                                        + tgtSecNameFull + ".v("+ fractionAlongTargetSection + ")" + "\n\n");
                        

//a_CG1[0].Soma { gap1 = new GapJunc(0.5) }

//setpointer gap1.vgap, a_CG1[1].Soma.v(0.5)
                    }


                }
            }
            
            if (simConfig.getMpiConf().isParallel() && addComments)
            {
                response.append("print \"++++++++++++\"\n");
                response.append("print \"Created netcons: \", allCurrentNetConns.count(), \" on host \", hostid\n");
                response.append("for c = 0,allCurrentNetConns.count()-1 {\n");
                response.append("   print \"Source of \", c,\": \", allCurrentNetConns.o(c).precell(), \", gid: \",allCurrentNetConns.o(c).srcgid()   \n");
                response.append("}\n");
                response.append("print \"++++++++++++\"\n");
            }
        }


        //GeneralUtils.timeCheck("Finsihed gen of syn conns, totNetConns: "+totNetConns+", response len: "+response.length()+ ", ratio: "+ (float)response.length()/totNetConns);
        GeneralUtils.timeCheck("Finsihed gen of syn conns");
        
        
        if (addComments && simConfig.getMpiConf().isParallel())
        {
            response.append("print \" -----------------------   Finished generation of net conns on host: \", hostid\n"); 
           // response.append("waittime = pnm.pc.barrier()\n"); 
           // response.append("print \"  Host: \", hostid, \" was waiting: \", waittime\n"); 
            /*response.append("netConInfoParallel(NetCon_10000000)\n"); 
            response.append("netConInfoParallel(NetCon_10000000_temp)\n"); 
            response.append("netConInfoParallel(NetCon_10000001)\n"); 
            response.append("netConInfoParallel(NetCon_10000001_temp)\n"); */
            response.append("print \""+preSectionsVsGids+"\"\n"); 
            response.append("print \"\"\n"); 
        }


        logger.logComment("Finsihed generation of the net conns");

        return response.toString();
    }

    public String generatePlots()
    {
        StringBuffer response = new StringBuffer();

        ArrayList<PlotSaveDetails> plots = project.generatedPlotSaves.getPlottedPlotSaves();

        addMajorHocComment(response, "Adding " + plots.size() + " plot(s)");

        for (PlotSaveDetails plot : plots)
        {
            ArrayList<Integer> cellNumsToPlot = plot.cellNumsToPlot;
            ArrayList<Integer> segIdsToPlot = plot.segIdsToPlot;

            String neuronVar = this.convertToNeuronVarName(plot.simPlot.getValuePlotted());

            float minVal = convertToNeuronValue(plot.simPlot.getMinValue(), plot.simPlot.getValuePlotted());
            float maxVal = convertToNeuronValue(plot.simPlot.getMaxValue(), plot.simPlot.getValuePlotted());

            Cell nextCell = project.cellManager.getCell(project.cellGroupsInfo.getCellType(plot.simPlot.getCellGroup()));

            for (Integer cellNum : cellNumsToPlot)
            {
                for (Integer segId: segIdsToPlot)
                {
                    Segment seg = nextCell.getSegmentWithId(segId);

                    float lenAlongSegment
                        = CellTopologyHelper.getFractionAlongSection(nextCell,
                                                                     seg,
                                                                     0.5f);

                    String title = "a_" + plot.simPlot.getCellGroup()
                        + "[" + cellNum + "]"
                        + "." + getHocSectionName(seg.getSection().getSectionName())
                        + "." + neuronVar;

                    String varRefIncFract = title + "(" + lenAlongSegment + ")";

                    if (plot.simPlot.isSynapticMechanism())
                    {
                        String netConn = SimPlot.getNetConnName(plot.simPlot.getValuePlotted());
                        
                        String synType = SimPlot.getSynapseType(plot.simPlot.getValuePlotted());

                        ArrayList<PostSynapticObject> synObjs = project.generatedNetworkConnections.getSynObjsPresent(netConn,
                                                                      synType,
                                                                      cellNum,
                                                                      segId);

                        logger.logComment("Syn objs for: "+netConn+", "+synType+", cellNum: "+cellNum
                                          +", segId: "+segId+": " + synObjs);

                        for (PostSynapticObject synObj: synObjs)
                        {
                            title = this.getSynObjName(synObj)+"."+neuronVar;
                            varRefIncFract = title;

                            response.append(generateSinglePlot(title,
                                                               plot.simPlot.getGraphWindow(),
                                                               minVal,
                                                               maxVal,
                                                               varRefIncFract,
                                                               getNextColour(plot.simPlot.getGraphWindow())));
                        }

                    }
                    else
                    {


                        response.append(generateSinglePlot(title,
                                                           plot.simPlot.getGraphWindow(),
                                                           minVal,
                                                           maxVal,
                                                           varRefIncFract,
                                                           getNextColour(plot.simPlot.getGraphWindow())));
                    }
                }
            }

        }

        

        return response.toString();
    }

    private String generateSinglePlot(String plotTitle,
                                      String graphWindow,
                                      float minVal,
                                      float maxVal,
                                      String varReference,
                                      String colour)
    {
        StringBuffer response = new StringBuffer();

        addHocComment(response,
                          " This code pops up a plot of " + varReference +"\n");

        if (!graphsCreated.contains(graphWindow))
        {
            response.append("objref " + graphWindow + "\n");
            response.append(graphWindow + " = new Graph(0)\n");
            response.append(graphWindow + ".size(0," + getSimDuration()
                            + "," + minVal
                            + "," + maxVal + ")\n");

            response.append(graphWindow + ".view(0, " + minVal + ", " + getSimDuration() +
                            ", " + (maxVal - minVal) + ", 80, 330, 330, 250)\n");

            graphsCreated.add(graphWindow);
        }

        response.append("{\n");

        response.append("    " + graphWindow + ".addexpr(\"" + plotTitle
                        + "\", \""+varReference+"\", " + colour
                        + ", 1, 0.8, 0.9, 2)\n");


        response.append("    " + "graphList[0].append(" + graphWindow + ")\n");
        response.append("}\n");

        return response.toString();

    }

    public String generateShapePlot()
    {
        StringBuffer response = new StringBuffer();

        addHocComment(response, " This code pops up a Shape plot of the cells\n");

        response.append("objref plotShape\n");
        response.append("plotShape = new PlotShape()\n");

        //response.append("plotShape.show(0)\n");
        response.append("plotShape.exec_menu(\"Shape Plot\")\n\n");
        response.append("fast_flush_list.append(plotShape)\n\n");


        return response.toString();
    }

    public String getNextColour(String plotFrame)
    {
        if (!nextColour.containsKey(plotFrame))
        {
            nextColour.put(plotFrame, 1);
        }
        int colNum = nextColour.get(plotFrame);
        
        String colour = colNum + "";
        int newColour = colNum +1;
        if (newColour >= 10) newColour = 1;
        
        nextColour.put(plotFrame, newColour);
        
        return colour;
    }

    public String generateRunMechanism()
    {
        StringBuffer response = new StringBuffer();

        String dateCommand = "date +%x,%X:%N";

        if (simConfig.getMpiConf().isParallel())
        {
            response.append("setuptime = stopsw()\n\n");
            response.append("print \"Setup time for simulation on host \",hostid,\": \",setuptime,\" seconds\"\n\n");
            
            response.append("pnm.want_all_spikes()\n");

            response.append("stdinit()\n");
            response.append("print \"Initialised on \", host\n");
            response.append("realtime = startsw()\n");
            response.append("pnm.psolve("+simConfig.getSimDuration()+")\n");
            response.append("realtime = startsw() - realtime\n");

            if (addComments) 
            {
                response.append("for i=0, pnm.spikevec.size-1 {\n");
                response.append("    print \"Spike \",i, \" at time \", pnm.spikevec.x[i],\" in cell: \", pnm.idvec.x[i]\n");
                response.append("}\n");
            }

            return response.toString();
        }


        //if (this.windowsTargetEnv()) dateCommand = "c:/windows/time.exe /T";
        
        boolean announceDate = !GeneralUtils.isWindowsBasedPlatform();
        String dateInfo = "";
        
        if(announceDate)
        {
    
            response.append("strdef date\n");
            response.append("system(\"" + dateCommand + "\", date)\n");
            dateInfo = " at time: \", date, \"";
        }
        response.append("setuptime = stopsw()\n\n");
        response.append("print \"Setup time for simulation: \",setuptime,\" seconds\"\n\n");

        response.append("print \"Starting simulation of duration "+simConfig.getSimDuration()+" ms, reference: " + project.simulationParameters.getReference() +
                dateInfo+"\"\n\n");

        response.append("startsw()\n\n");

        if (!project.neuronSettings.isVarTimeStep())
        {
            addMajorHocComment(response, "Main run statement");
            response.append("run()\n\n");
        }
        else
        {
            addMajorHocComment(response, "Main run statement");
            addHocComment(response, "Setting basic variable time step active");

            response.append("cvode.active(1)\n");
            response.append("run()\n\n");

        }
        dateInfo = "";
        
        if(announceDate)
        {
            response.append("system(\"" + dateCommand + "\", date)\n");
            dateInfo = "print \"Current time: \", date\n\n";
        }

        response.append("print \"Finished simulation in \", realtime ,\"seconds\"\n\n");
        response.append(dateInfo);

        return response.toString();

    }





    public String generateGUIForRerunning()
    {
        StringBuffer response = new StringBuffer();

        addHocComment(response, " This code pops up a simple Run Control\n");

        response.append("{\n");
        response.append("xpanel(\"RunControl\", 0)\n");
        response.append("v_init = " + project.simulationParameters.getInitVm() + "\n");
        //response.append("xvalue(\"Init\",\"v_init\", 1,\"stdinit()\", 1, 1 )\n");
        response.append("xbutton(\"Init & Run\",\"run()\")\n");
        response.append("xbutton(\"Stop\",\"stoprun=1\")\n");
        response.append("t = 0\n");
        response.append("xvalue(\"t\",\"t\", 2 )\n");
        response.append("tstop = " + getSimDuration() + "\n");
        response.append("xvalue(\"Tstop\",\"tstop\", 1,\"tstop_changed()\", 0, 1 )\n");
        response.append("dt = " + project.simulationParameters.getDt() + "\n");
        response.append(" xvalue(\"dt\",\"dt\", 1,\"setdt()\", 0, 1 )\n");
        response.append("xpanel(80,80)\n");
        response.append("}\n\n");

        return response.toString();
    }

    private String generateQuit()
    {
        StringBuffer response = new StringBuffer();

        addHocComment(response,
                          " As it is intended to run the file under Condor, the hoc will quit after finishing\n");

        response.append("\nquit()\n");

        return response.toString();
    }
    
    public static boolean isRunModePythonBased(int runMode)
    {
        return (runMode == RUN_PYTHON_XML || runMode == RUN_PYTHON_HDF5) ;
    }


    public void addComment(StringBuffer responseBuffer, String comment)
    {
        if (!isRunModePythonBased(genRunMode))
            addHocComment(responseBuffer, comment);
        else
            PythonUtils.addComment(responseBuffer, comment, addComments);
    }

    public static void addHocComment(StringBuffer responseBuffer, String comment)
    {
        if (!addComments) return;

        //comment = GeneralUtils.replaceAllTokens(comment, "\n", "\n//  ");
        
        addHocComment(responseBuffer, comment, "", true);
    }
    
    public static void addHocComment(StringBuffer responseBuffer, String comment, boolean inclReturn)
    {
        if (!addComments) return;
        addHocComment(responseBuffer, comment, "", inclReturn);
    }


    public static void addHocComment(StringBuffer responseBuffer, String comment, String preSlashes, boolean inclReturn)
    {
        if (!addComments) return;
        String pre = preSlashes+ "//  ";
        
        
        if (!comment.toString().endsWith("\n")) comment = comment +"\n";

        comment = GeneralUtils.replaceAllTokens(comment.substring(0,comment.length()-1), "\n", "\n"+pre) + "\n";

        
        responseBuffer.append("\n" + pre + comment);
        if (inclReturn) responseBuffer.append("\n");
    }
    
    public void addMajorComment(StringBuffer responseBuffer, String comment)
    {
        if (!addComments)return;
        
        if (!isRunModePythonBased(genRunMode)) 
            addMajorHocComment(responseBuffer, comment);
        else
            PythonUtils.addMajorComment(responseBuffer, comment);
    }

    public static void addMajorHocComment(StringBuffer responseBuffer, String comment)
    {
        if (!addComments)return;
        if (!responseBuffer.toString().endsWith("\n")) responseBuffer.append("\n");
        responseBuffer.append("//////////////////////////////////////////////////////////////////////\n");
        responseBuffer.append("//   " + comment + "\n");
        responseBuffer.append("//////////////////////////////////////////////////////////////////////\n");
        responseBuffer.append("\n");
    }


    public static float convertToNeuronValue(Float val, String simIndepVarName)
    {
        if (simIndepVarName.equals(SimPlot.VOLTAGE))
        {
            return (float) UnitConverter.getVoltage(val,
                                                    UnitConverter.NEUROCONSTRUCT_UNITS,
                                                    UnitConverter.NEURON_UNITS);
        }
        if (simIndepVarName.indexOf(SimPlot.SPIKE)>=0)
        {
            return (float) UnitConverter.getVoltage(val,
                                                    UnitConverter.NEUROCONSTRUCT_UNITS,
                                                    UnitConverter.NEURON_UNITS);
        }
        else if (simIndepVarName.indexOf(SimPlot.COND_DENS) >= 0)
        {
            return (float) UnitConverter.getConductanceDensity(val,
                                                               UnitConverter.NEUROCONSTRUCT_UNITS,
                                                               UnitConverter.NEURON_UNITS);
        }
        else if (simIndepVarName.indexOf(SimPlot.CONCENTRATION)>=0)
        {
            /** @todo Check this... */
            return (float) UnitConverter.getConcentration(val,
                                                          UnitConverter.NEUROCONSTRUCT_UNITS,
                                                          UnitConverter.NEURON_UNITS);
        }
        else if (simIndepVarName.indexOf(SimPlot.CURRENT)>=0)
        {
            return (float) UnitConverter.getCurrentDensity(val,
                                                           UnitConverter.NEUROCONSTRUCT_UNITS,
                                                           UnitConverter.NEURON_UNITS);
        }
        else if (simIndepVarName.equals(SimPlot.REV_POT))
        {
            return (float) UnitConverter.getVoltage(val,
                                                    UnitConverter.NEUROCONSTRUCT_UNITS,
                                                    UnitConverter.NEURON_UNITS);
        }
        else if (simIndepVarName.indexOf(SimPlot.SYN_COND)>=0)
        {
            return (float) UnitConverter.getConductance(val,
                                                        UnitConverter.NEUROCONSTRUCT_UNITS,
                                                        UnitConverter.NEURON_UNITS);
        }
        return val;
    }



    public String convertToNeuronVarName(String simIndepVarName)
    {
        String neuronVar = null;
        String origIndepName = new String(simIndepVarName);

        if (simIndepVarName.equals(SimPlot.VOLTAGE))
        {
            neuronVar = "v";
        }
        else if (simIndepVarName.indexOf(SimPlot.SPIKE)>=0)
        {
            // only used when plotting a spike, when saving, an aPCount is used.
            neuronVar = "v";
        }
        else if (simIndepVarName.indexOf(SimPlot.PLOTTED_VALUE_SEPARATOR) > 0)
        {
            boolean isSyn = false;
            if (simIndepVarName.indexOf(SimPlot.SYNAPSES)>=0)
            {
                simIndepVarName = simIndepVarName.substring(SimPlot.SYNAPSES.length()+
                                                            SimPlot.PLOTTED_VALUE_SEPARATOR.length());
                isSyn = true;
            }

            String mechanismName = simIndepVarName.substring(0,
                                                      simIndepVarName.indexOf(SimPlot.
                                                                         PLOTTED_VALUE_SEPARATOR));


            String variable = simIndepVarName.substring(
                simIndepVarName.indexOf(SimPlot.PLOTTED_VALUE_SEPARATOR) + 1);

            logger.logComment("--------------   Original: "+origIndepName+", so looking to plot " 
                + variable +" on cell mechanism: " + mechanismName+", simIndepVarName: "+simIndepVarName);

            if (variable.startsWith(SimPlot.COND_DENS))
            {
                variable = "gion";

                neuronVar = variable + "_" + mechanismName;

            }
            else if (variable.indexOf(SimPlot.SYN_COND)>=0)
            {
                neuronVar = "g";

            }
            else if (isSyn)
            {
                neuronVar = simIndepVarName.substring(simIndepVarName.lastIndexOf(SimPlot.
                                                                         PLOTTED_VALUE_SEPARATOR)+1);
                
                logger.logComment("neuronVar: "+neuronVar);
            }
            else
            {
                CellMechanism cp = project.cellMechanismInfo.getCellMechanism(mechanismName);

                logger.logComment("Cell mech found: " + cp);

                if (cp == null)
                {
                    GuiUtils.showErrorMessage(logger,
                                              "Problem generating plot with Cell Mechanism: " +
                                              mechanismName, null, null);

                    return null;

                    //neuronVar = variable + "_" + mechanismName;
                }
                else
                {

                    if (cp instanceof ChannelMLCellMechanism)
                    {
                        try
                        {
                            if (variable.startsWith(SimPlot.CONCENTRATION))
                            {
                                logger.logComment("Looking to plot the concentration...");

                                String ion = variable.substring(variable.indexOf(SimPlot.
                                    PLOTTED_VALUE_SEPARATOR) + 1);

                                neuronVar = ion + "i"; // assume internal concentration
                            }
                            else if (variable.startsWith(SimPlot.CURRENT))
                            {
                                logger.logComment("Looking to plot the current...");

                                String ion = variable.substring(variable.indexOf(SimPlot.
                                    PLOTTED_VALUE_SEPARATOR) + 1);

                                neuronVar = "i" + ion;
                            }
                            else if (variable.startsWith(SimPlot.REV_POT))
                            {
                                logger.logComment("Looking to plot the reversal potential...");

                                String ion = variable.substring(variable.indexOf(SimPlot.
                                    PLOTTED_VALUE_SEPARATOR) + 1);

                                neuronVar = "e" + ion;
                            }


                            else
                            {
                                logger.logComment(
                                    "Assuming using the native name of the variable");
                                neuronVar = variable + "_" + mechanismName;
                            }
                        }
                        catch (Exception ex)
                        {
                            GuiUtils.showErrorMessage(logger,
                                                      "Problem generating a plot with Cell Mechanism: " +
                                                      mechanismName, ex, null);
                            return null;

                        }

                    }
                    else
                    {
                        logger.logError("Unsupported type of Cell Mechanism");

                        neuronVar = variable + "_" + mechanismName;

                    }
                }
            }
        }
        else
        {
            // use the name itself...
            neuronVar = simIndepVarName;
        }


        return neuronVar;
    }


    public void runNeuronFile(File mainHocFile) throws NeuronException
    {
        logger.logComment("Trying to run the hoc file: "+ mainHocFile);

        if (!mainHocFile.exists())
        {
            throw new NeuronException("The NEURON file: "+ mainHocFile
                                      + " does not exist. Have you generated the NEURON code?");
        }


        logger.logComment("Getting rid of old simulation files...");

        File dirForDataFiles = mainHocFile.getParentFile();
        

        File[] filesInDir = dirForDataFiles.listFiles();

        logger.logComment("Files in dir: "+ dirForDataFiles.getAbsolutePath());
        for (int i = 0; i < filesInDir.length; i++)
        {
            logger.logComment("File "+i+": "+filesInDir[i]);
        }

        Runtime rt = Runtime.getRuntime();
        
        String fullCommand = "";

        if (genRunMode!=RUN_VIA_CONDOR)
        {
            try
            {
                String locationOfNeuron = GeneralProperties.getNeuronHomeDir();

                String neuronExecutable = null;

                if (GeneralUtils.isWindowsBasedPlatform())
                {
                    logger.logComment("Assuming Windows environment...");
                    
                    neuronExecutable = locationOfNeuron
                        + System.getProperty("file.separator")
                        + "bin"
                        + System.getProperty("file.separator")
                        + "neuron.exe";
                    
                    String filename = getHocFriendlyFilename(mainHocFile.getAbsolutePath());

                    if (filename.indexOf(" ")>=0)
                    {
                        GuiUtils.showErrorMessage(logger, "Error. The full name of the file to execute in NEURON: "+filename
                                                  +" contains a space. This will throw an error in NEURON.\n Was the code created in a directory containing a space in its name?", null, null);

                    }

                    fullCommand = GeneralProperties.getExecutableCommandLine() + " "
                    	+ neuronExecutable + " "+filename;
                  


                    File dirToRunIn = dirForDataFiles;


                    String scriptText = "cd "+dirToRunIn.getAbsolutePath()+"\n";
                  
                    scriptText = scriptText + fullCommand;
                    
                    File scriptFile = new File(ProjectStructure.getNeuronCodeDir(project.getProjectMainDirectory()), "runsim.bat");
                    FileWriter fw = new FileWriter(scriptFile);
                    fw.write(scriptText);
                    fw.close();

                    logger.logComment("Going to execute command: " + fullCommand + " in dir: " +
                                      dirToRunIn);


                    rt.exec(fullCommand, null, dirToRunIn);
                    

                    logger.logComment("Have executed command: " + fullCommand + " in dir: " +
                                      dirToRunIn);

                }
                else
                {
                    String[] commandToExe = null;
                    String[] envParams = null;
                    
                    //TODO: Make this an input option!!!
                    if (genRunMode==RUN_PYTHON_HDF5)
                    {
                        envParams = new String[1];
                        envParams[0] = "LD_LIBRARY_PATH=/usr/local/hdf5/lib";
                    }
                    
                    if (dirForDataFiles.getAbsolutePath().indexOf(" ")>=0)
                    {
                        throw new NeuronException("NEURON files cannot be run in a directory like: "+ dirForDataFiles
                                + " containing spaces.\nThis is due to the way neuroConstruct starts the external processes (e.g. konsole) to run NEURON.\nArguments need to be given to this executable and spaces in filenames cause problems.\n"
                                +"Try saving the project in a directory without spaces.");
                    }
                    
                    String mainExecutable = "nrngui";
                    
                    if (simConfig.getMpiConf().isParallel()) mainExecutable = "nrniv";

                    neuronExecutable = locationOfNeuron
                        + System.getProperty("file.separator")
                        + "bin"
                        + System.getProperty("file.separator")
                        + mainExecutable;

                    String title = "NEURON_simulation" + "__"+ project.simulationParameters.getReference();

                    File dirToRunInFile = dirForDataFiles;
                    
                    String dirToRunInPath = dirToRunInFile.getAbsolutePath();


                    String basicCommLine = GeneralProperties.getExecutableCommandLine();

                    String executable = "";
                    String extraArgs = "";
                    String titleOpt = "";
                    String workdirOpt = "";
                    String postArgs = "";
                    
                    String mpiFlags = "";

                    StringBuffer preCommand = new StringBuffer("");

                    if (simConfig.getMpiConf().isParallel())
                    {
                        MpiSettings mpiSets = new MpiSettings();
                        
                        String hostFlag = "-map";
                        String hostSeperator = ":";
                        
                        if (mpiSets.getVersion().equals(MpiSettings.MPI_V2))
                        {
                            hostFlag = "-host";
                            hostSeperator = ",";
                            mpiFlags = "-mpi ";
                        }
                        preCommand.append("mpirun "+hostFlag+" ");

                        ArrayList<MpiHost> hosts = simConfig.getMpiConf().getHostList();

                        for (int i = 0; i < hosts.size(); i++)
                        {
                            for (int j = 0; j < hosts.get(i).getNumProcessors(); j++)
                            {
                                if (!(i==0 && j==0)) preCommand.append(hostSeperator);
                                preCommand.append(hosts.get(i).getHostname());
                            }
                        }
                        preCommand.append("  ");

                    }

                    File scriptFile = new File(ProjectStructure.getNeuronCodeDir(project.getProjectMainDirectory()),
                                               "runsim.sh");
                    
                    if (GeneralUtils.isLinuxBasedPlatform())
                    {
                        logger.logComment("Is linux platform...");

                        if (basicCommLine.indexOf("konsole") >= 0)
                        {
                            logger.logComment("Assume we're using KDE");
                            titleOpt = "-T";
                            workdirOpt = "--workdir";
                               // + dirToRunIn.getAbsolutePath();
                            
                            extraArgs = "-e";
                            executable = basicCommLine.trim();
                        }
                        else if (basicCommLine.indexOf("gnome") >= 0)
                        {
                            logger.logComment("Assume we're using Gnome");
                            titleOpt = "--title";
                            workdirOpt = "--working-directory";

                            if (basicCommLine.trim().indexOf(" ") > 0) // case where basicCommLine is gnome-terminal -x
                            {
                                extraArgs = basicCommLine.substring(basicCommLine.trim().indexOf(" ")).trim();

                                executable = basicCommLine.substring(0, basicCommLine.trim().indexOf(" ")).trim();
                            }
                            else
                            {
                                extraArgs = "-x";
                            }

                        }
                        else
                        {
                            logger.logComment("Unknown console command, going with the flow...");

                            executable = basicCommLine.trim();
                        }

                        commandToExe = new String[]{executable, 
                                titleOpt, title,
                                workdirOpt, dirToRunInPath,
                                extraArgs,
                                scriptFile.getAbsolutePath()};
                    }
                    else if (GeneralUtils.isMacBasedPlatform())
                    {
                            logger.logComment("Assuming a Mac based machine...");

                            executable = basicCommLine.trim();

                            /** @todo update with real command line option for working dir... */
                            workdirOpt = " ";


                            postArgs = "";

                            dirToRunInFile = ProjectStructure.getNeuronCodeDir(project.getProjectMainDirectory());
                            dirToRunInPath = "";
                            title = "";
                            

                            commandToExe = new String[]{executable, 
                                    scriptFile.getAbsolutePath()};
                    }

                    String scriptText = "cd '" + dirToRunInFile.getAbsolutePath() + "'\n";
                    
                    if (true || !isRunModePythonBased(genRunMode))
                    {
                        scriptText = scriptText + preCommand
                        + neuronExecutable
                        + " "
                        + mpiFlags
                        + mainHocFile.getName()
                        + postArgs;
                    }
                    else
                    {
                        scriptText = scriptText + "python "+ runPythonFile.getName();
                    }

                    
                    FileWriter fw = new FileWriter(scriptFile);
                    //scriptFile.se
                    fw.write(scriptText);
                    fw.close();

                    // bit of a hack...
                    rt.exec(new String[]{"chmod","u+x",scriptFile.getAbsolutePath()});
                    
                    try
                    {
                        // This is to make sure the file permission is updated..
                        Thread.sleep(600);
                    }
                    catch (InterruptedException ex)
                    {
                        ex.printStackTrace();
                    }


                    fullCommand = "";
                    for (int i=0;i<commandToExe.length;i++)
                    {
                        fullCommand = fullCommand+" "+ commandToExe[i];
                    }

                    logger.logComment("Going to execute command: " + fullCommand);

                    rt.exec(commandToExe, envParams);

                    logger.logComment("Have successfully executed command: " + fullCommand);

                }

            }
            catch (Exception ex)
            {
                logger.logError("Error running the command: " + fullCommand, ex);
                throw new NeuronException("Error executing the hoc file: " + mainHocFile+"\n"+ex.getMessage(), ex);
            }
        }
        else
        {
            logger.logComment("Creating the extra files for running the code through Condor...");
            FileWriter condorBatchFileWriter = null;
            FileWriter condorSubmitFileWriter = null;
            File condorSubmitFile = null;
            File condorBatchFile = null;


            File dirForSimFiles = ProjectStructure.getDirForSimFiles(project.simulationParameters.getReference(), project);

            try
            {
                //String systemOS = System.getProperty("os.name");

                if (GeneralUtils.isWindowsBasedPlatform())
                {

                    condorBatchFile = new File(dirForSimFiles, project.simulationParameters.getReference() + ".bat");
                    condorBatchFileWriter = new FileWriter(condorBatchFile);

                    condorSubmitFile = new File(dirForSimFiles, project.simulationParameters.getReference() + ".sub");
                    condorSubmitFileWriter = new FileWriter(condorSubmitFile);

                    condorSubmitFileWriter.write("universe = vanilla\n");
                    //fw.write("environment = path=c:\WINDOWS\SYSTEM32;C:\nrn55\bin\n"):
                    condorSubmitFileWriter.write("executable = " + condorBatchFile.getName() + "\n");
                    condorSubmitFileWriter.write("arguments = " + mainHocFile.getName() + "\n");
                    condorSubmitFileWriter.write("initialdir = " + dirForSimFiles.getAbsolutePath() + "\n");


                    condorSubmitFileWriter.write("transfer_input_files = nrnmech.dll");// + mainHocFile.getName());

                    File[] allHocFiles
                        = mainHocFile.getParentFile().listFiles(new SimpleFileFilter(new String[]{".hoc"}, ""));

                    for (int i = 0; i < allHocFiles.length; i++)
                    {
                        condorSubmitFileWriter.write(", " + allHocFiles[i].getName());

                    }


                    for (int i = 0; i < cellTemplatesGenAndIncluded.size(); i++)
                    {
                        String nextHocFile = cellTemplatesGenAndIncluded.elementAt(i);
                        condorSubmitFileWriter.write(", " + (new File(nextHocFile)).getName());
                    }

                    //condorSubmitFileWriter.write(", nCtools.hoc");
                    condorSubmitFileWriter.write("\n");

                    condorSubmitFileWriter.write("should_transfer_files = YES\n");
                    condorSubmitFileWriter.write("when_to_transfer_output = ON_EXIT\n");
                    condorSubmitFileWriter.write("transfer_files = ALWAYS\n");
                    condorSubmitFileWriter.write("output = nrn.out\n");
                    condorSubmitFileWriter.write("error = nrn.err\n");
                    condorSubmitFileWriter.write("log = nrn.log\n");
                    condorSubmitFileWriter.write("queue\n");

                    condorSubmitFileWriter.flush();
                    condorSubmitFileWriter.close();

                    condorBatchFileWriter.write("C:\\WINDOWS\\SYSTEM32\\cmd /C "
                                                + GeneralProperties.getNeuronHomeDir()
                                                + System.getProperty("file.separator")
                                                + "bin"
                                                + System.getProperty("file.separator")
                                                + "nrniv.exe %1\n");

                    condorBatchFileWriter.flush();
                    condorBatchFileWriter.close();

                    logger.logComment("Assuming Windows environment...");
                    String executable = "condor_submit " + condorSubmitFile.getName();

                    File dirToRunIn = dirForSimFiles;

                    logger.logComment("Going to execute: " + executable + " in dir: " +
                                      dirToRunIn);


                    rt.exec(executable, null, dirToRunIn);
                    logger.logComment("Have successfully executed command: " + executable + " in dir: " +
                                      dirToRunIn);




                }
                else
                {

                    condorBatchFile = new File(dirForSimFiles, project.simulationParameters.getReference() + ".sh");
                    condorBatchFileWriter = new FileWriter(condorBatchFile);

                    condorSubmitFile = new File(dirForSimFiles, project.simulationParameters.getReference() + ".sub");
                    condorSubmitFileWriter = new FileWriter(condorSubmitFile);

                    condorSubmitFileWriter.write("universe = vanilla\n");
                    //fw.write("environment = path=c:\WINDOWS\SYSTEM32;C:\nrn55\bin\n"):
                    //condorSubmitFileWriter.write("executable = " + condorBatchFile.getName() + "\n");
                    condorSubmitFileWriter.write("executable = /bin/bash\n");
                    //condorSubmitFileWriter.write("arguments = " + mainHocFile.getName() + "\n");
                    condorSubmitFileWriter.write("arguments = " + condorBatchFile.getName() + "\n");
                    condorSubmitFileWriter.write("initialdir = " + dirForSimFiles.getAbsolutePath() + "\n");


                    //condorSubmitFileWriter.write("transfer_input_files = nrnmech.dll");// + mainHocFile.getName());
                    condorSubmitFileWriter.write("transfer_input_files = "+condorBatchFile.getName());

                    //StringBuffer file

                    File[] allHocFiles
                        = mainHocFile.getParentFile().listFiles(new SimpleFileFilter(new String[]{".hoc"}, ""));

                    for (int i = 0; i < allHocFiles.length; i++)
                    {
                        //System.out.println("Looking at: "+ allHocFiles[i].getAbsolutePath());
                        if (!allHocFiles[i].isDirectory())
                        {
                            condorSubmitFileWriter.write(", ");
                            condorSubmitFileWriter.write(allHocFiles[i].getName());
                        }
                    }

                    File libsDir = new File(dirForSimFiles, GeneralUtils.getArchSpecificDir());

                    // Messy roundabout way to do it...
                    File tempDir = new File(dirForSimFiles, "temp");
                    tempDir.mkdir();
                    File tempLibsDir = new File(tempDir, GeneralUtils.getArchSpecificDir());
                    tempLibsDir.mkdir();


                    GeneralUtils.copyDirIntoDir(libsDir, tempLibsDir, true, true);

                    String zippedLibsFilename = GeneralUtils.getArchSpecificDir() + ".zip";

                    File zipFile = new File(dirForSimFiles, zippedLibsFilename);

                    zipFile = ZipUtils.zipUp(tempDir, 
                            zipFile.getAbsolutePath(),
                            new ArrayList<String>(),
                            new ArrayList<String>());

                    //System.out.println("Zip file created with mod libs: "+ zipFile.getAbsolutePath());

                    condorSubmitFileWriter.write(", "+ zipFile.getName());


                    condorSubmitFileWriter.write(", nCtools.hoc");
                    condorSubmitFileWriter.write("\n");

                    condorSubmitFileWriter.write("should_transfer_files = YES\n");
                    condorSubmitFileWriter.write("when_to_transfer_output = ON_EXIT\n");
                    condorSubmitFileWriter.write("transfer_files = ALWAYS\n");
                    condorSubmitFileWriter.write("output = nrn.out\n");
                    condorSubmitFileWriter.write("error = nrn.err\n");
                    condorSubmitFileWriter.write("log = nrn.log\n");
                    condorSubmitFileWriter.write("queue\n");

                    condorSubmitFileWriter.flush();
                    condorSubmitFileWriter.close();

                    //condorBatchFileWriter.write("C:\\WINDOWS\\SYSTEM32\\cmd /C "
                    //                            + GeneralProperties.getNeuronHomeDir()
                    //                            + System.getProperty("file.separator")
                     //                           + "bin"
                    //                            + System.getProperty("file.separator")
                   //                             + "nrniv.exe %1\n");

                   //condorBatchFileWriter.write("mkdir i686\n");
                   //condorBatchFileWriter.write("mv \n");
                   condorBatchFileWriter.write("unzip "+zippedLibsFilename+"\n");
                   condorBatchFileWriter.write("chmod -R a+x "+GeneralUtils.getArchSpecificDir()+"\n");


                   // for testing...
                   condorBatchFileWriter.write("echo Current dir structure:\n");
                   condorBatchFileWriter.write("ls -altR\n");
                   //condorBatchFileWriter.write("sleep 4\n");
                   //condorBatchFileWriter.write("cp -Rv . /home/condor/temp/cop\n");


                   condorBatchFileWriter.write("echo\n");
                   condorBatchFileWriter.write("echo Starting NEURON...\n");

                   condorBatchFileWriter.write("/usr/local/nrn/"+GeneralUtils.getArchSpecificDir()+"/bin/nrngui "+mainHocFile.getName()+"\n");



                    condorBatchFileWriter.flush();
                    condorBatchFileWriter.close();

                    logger.logComment("Assuming *nix environment...");
                    String executable = "condor_submit " + condorSubmitFile.getName();

                    File dirToRunIn = dirForSimFiles;

                    logger.logComment("Going to execute: " + executable + " in dir: " +
                                      dirToRunIn);


                    rt.exec(executable, null, dirToRunIn);
                    logger.logComment("Have successfully executed command: " + executable + " in dir: " +
                                      dirToRunIn);


                }

            }
            catch (Exception ex)
            {
                GuiUtils.showErrorMessage(logger, "Error writing to file: " + condorSubmitFile , ex, null);
                try
                {
                    condorSubmitFileWriter.close();
                    condorBatchFileWriter.close();
                }
                catch (IOException ex1)
                {
                }
                catch (NullPointerException ex1)
                {
                }

                return;
            }

        }

    }


    public File getMainHocFile() throws NeuronException
    {
        if (!this.hocFileGenerated)
        {
            logger.logError("Trying to run without generating first");
            throw new NeuronException("Hoc file not yet generated");
        }

        return this.mainHocFile;

    }


    public static  void main(String[] args)
    {
        try
        {

            int runMode  = RUN_HOC;


            MainFrame frame = new MainFrame();

           // File pf = new File("/bernal/models/Parallel/Parallel.neuro.xml");
            File pf = new File("/home/padraig/nC_projects/Project_1ppp/Project_1ppp.neuro.xml");

            //File pf = new File("models/PVMExample/PVMExample.neuro.xml");

            frame.doLoadProject(pf.getAbsolutePath());

            System.out.println("doGenerate...");
            frame.projManager.doGenerate("Default Simulation Configuration", 1234);

            System.out.println("Snoozing...");

            Thread.sleep(500);

            System.out.println("Coming out of sleep");

            frame.doCreateHoc(runMode);

            System.out.println("done create");


            frame.projManager.getCurrentProject().neuronFileManager.runNeuronFile(
                frame.projManager.getCurrentProject().neuronFileManager.getMainHocFile());

            System.exit(0);

        }
        catch(Exception e)
        {
            e.printStackTrace();
        }


    }
    public Vector<String> getModFilesToCompile()
    {
        Vector<String> allMods = new Vector<String>(this.stimModFilesRequired);
        
        allMods.addAll(cellMechFilesGenAndIncl);
        

        return allMods;
    }

}
