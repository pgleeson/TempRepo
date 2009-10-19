/**
 *  neuroConstruct
 *  Software for developing large scale 3D networks of biologically realistic neurons
 * 
 *  Copyright (c) 2009 Padraig Gleeson
 *  UCL Department of Neuroscience, Physiology and Pharmacology
 *
 *  Development of this software was made possible with funding from the
 *  Medical Research Council and the Wellcome Trust
 *  
 *  This program is free software; you can redistribute it and/or modify
 *  it under the terms of the GNU General Public License as published by
 *  the Free Software Foundation; either version 2 of the License, or
 *  (at your option) any later version.
 *  
 *  This program is distributed in the hope that it will be useful,
 *  but WITHOUT ANY WARRANTY; without even the implied warranty of
 *  MERCHANTABILITY or FITNESS FOR A PARTICULAR PURPOSE.  See the
 *  GNU General Public License for more details.

 *  You should have received a copy of the GNU General Public License
 *  along with this program; if not, write to the Free Software
 *  Foundation, Inc., 59 Temple Place, Suite 330, Boston, MA  02111-1307  USA
 *
 */

package ucl.physiol.neuroconstruct.simulation;

import java.io.*;
import ucl.physiol.neuroconstruct.utils.*;
import java.util.*;
import java.text.*;
import ucl.physiol.neuroconstruct.dataset.DataSet;
import ucl.physiol.neuroconstruct.utils.units.*;
import ucl.physiol.neuroconstruct.project.SimPlot;
import ucl.physiol.neuroconstruct.project.PostSynapticObject;

/**
 * Class which stores all the data from a simulation, including references to data files
 *
 * @author Padraig Gleeson
 *  
 */

public class SimulationData
{
    private static ClassLogger logger = new ClassLogger("SimulationData");


    private ArrayList<DataStore> dataSources = new ArrayList<DataStore>();

    private ArrayList<String> allCellRefsCached = null;
    private ArrayList<String> allVoltCellRefsCached = null;

    private double[] times = null;

    private static final String TIME_DATA_FILE_STD = "time."+SimPlot.CONTINUOUS_DATA_EXT;
    private static final String TIME_DATA_FILE_PSICS = "time.txt";
    public static final String POSITION_DATA_FILE = "CellPositions."+SimPlot.CONTINUOUS_DATA_EXT;
    public static final String NETCONN_DATA_FILE = "NetworkConnections."+SimPlot.CONTINUOUS_DATA_EXT;
    public static final String ELEC_INPUT_DATA_FILE = "ElectricalInputs."+SimPlot.CONTINUOUS_DATA_EXT;

    private boolean dataLoaded = false;

    private File dataDirectory = null;
    //private File timesDataFile = null;

    private static int suggestedInitCapData = 100000;
    private static int suggestedInitCapSpikes = 50;

    /**
     * Gives an indication whether the voltages are present for just the soma or lots of segs...
     */
    private boolean onlySomaValues = true;

    private boolean dataAtRemoteLocation = false;


    private SimulationData()
    {
    }


    public SimulationData(File dataDirectory) throws SimulationDataException
    {
        this(dataDirectory, true);
    }

    public SimulationData(File dataDirectory, boolean checkTimesFile) throws SimulationDataException
    {
        //logger.setThisClassVerbose(true);

        logger.logComment("New SimulationData created with data dir: " + dataDirectory);

        if (!dataDirectory.exists() || !dataDirectory.isDirectory())
            throw new SimulationDataException(dataDirectory.getAbsolutePath()+ " is not a directory");

        this.dataDirectory = dataDirectory;



        if (checkTimesFile && !getTimesFile().exists())
            throw new SimulationDataException(getTimesFile().getAbsolutePath()+ " (time data) not found");


    }

    public boolean isDataAtRemoteLocation()
    {
        return dataAtRemoteLocation;
    }

    public void setDataAtRemoteLocation(boolean dataAtRemoteLocation)
    {
        this.dataAtRemoteLocation = dataAtRemoteLocation;
    }


    
    public File getTimesFile()
    {
        return getTimesFile(dataDirectory);
    }
    
    public static File getTimesFile(File simDir)
    {
        File psicsFile = new File(simDir,TIME_DATA_FILE_PSICS);
        if (psicsFile.exists())
            return psicsFile;
        return new File(simDir,TIME_DATA_FILE_STD);
    }
    public static String getStandardTimesFilename()
    {
        return TIME_DATA_FILE_STD;
    }

    public void reset()
    {
        this.allCellRefsCached = null;
        this.allVoltCellRefsCached = null;
        this.dataSources.clear();
        times = null;
        dataLoaded = false;
    }

    public void initialise()  throws SimulationDataException
    {
        if (dataLoaded)
        {
            logger.logComment("initialise() called but dataLoaded...");
            return;
        }
        this.reset();

        logger.logComment("++--+   Initialising SimulationData "+this.hashCode()+" with directory: "+ dataDirectory.getAbsolutePath());

        //GeneralUtils.timeCheck("Starting reading times file");

        Properties props = SimulationsInfo.getSimulationProperties(dataDirectory);

        double timeConversionFactor = 1;

        String unitSystemDesc = props.getProperty("Unit system");
        logger.logComment("unitSystemDesc: "+ unitSystemDesc);

        int unitSystem = UnitConverter.getUnitSystemIndex(unitSystemDesc);

        timeConversionFactor = UnitConverter.getTime(timeConversionFactor,
                                                     unitSystem,
                                                     UnitConverter.NEUROCONSTRUCT_UNITS);


        times = readDataFileToArray(getTimesFile(), timeConversionFactor);
        logger.logComment("There are "+times.length+" entries in the time file");

        //GeneralUtils.timeCheck("Starting reading voltages");

        File[] cellDataFiles = dataDirectory.listFiles(
            new FilenameFilter()
        {
            public boolean accept(File dir, String name)
            {
                if ( (name.endsWith("." + SimPlot.CONTINUOUS_DATA_EXT) || 
                    name.endsWith("." + SimPlot.CONTINUOUS_DATA_EXT+".txt") || /* TEMP for PSICS!!*/
                    name.endsWith("." + SimPlot.SPIKE_EXT))
                    && !name.equals(TIME_DATA_FILE_STD)
                    && !name.equals(TIME_DATA_FILE_PSICS)
                    && !name.equals(POSITION_DATA_FILE)
                    && !name.equals(NETCONN_DATA_FILE)
                    && !name.equals(ELEC_INPUT_DATA_FILE)
                    && name.indexOf("psics-out")<0)
                {
                    logger.logComment("-----   Taking " + name);
                    return true;
                }
                else
                {
                    logger.logComment("-----   Rejecting " + name);
                    return false;
                }

            }
        });

        logger.logComment("Found: " + cellDataFiles.length);

        for (int fileIndex = 0; fileIndex < cellDataFiles.length; fileIndex++)
        {
            logger.logComment("-----   Looking at "+fileIndex+": "+cellDataFiles[fileIndex]);

            String dataSourceName = null;

            if (cellDataFiles[fileIndex].getName().indexOf("."+SimPlot.CONTINUOUS_DATA_EXT+".txt")>0)
            {
                dataSourceName = cellDataFiles[fileIndex].getName().substring(0, cellDataFiles[fileIndex].getName().length()
                                                                      - ("." + SimPlot.CONTINUOUS_DATA_EXT+".txt").length());
            }
            else if (cellDataFiles[fileIndex].getName().indexOf("."+SimPlot.CONTINUOUS_DATA_EXT)>0)
            {
                dataSourceName = cellDataFiles[fileIndex].getName().substring(0, cellDataFiles[fileIndex].getName().length()
                                                                      - ("." + SimPlot.CONTINUOUS_DATA_EXT).length());
            }
            else if (cellDataFiles[fileIndex].getName().indexOf("."+SimPlot.SPIKE_EXT)>0)
            {
                dataSourceName = cellDataFiles[fileIndex].getName().substring(0, cellDataFiles[fileIndex].getName().length()
                                                                      - ("." + SimPlot.SPIKE_EXT).length());
            }

            logger.logComment("dataSourceName: "+dataSourceName);

            String variable = getVariable(dataSourceName);

            logger.logComment("variable: "+variable);

            String cellSegRef = getCellSegRef(dataSourceName);

            logger.logComment("cellSegRef: "+cellSegRef);


            PostSynapticObject pso = getPostSynapticObject(dataSourceName);

            logger.logComment("PostSynapticObject: "+pso);

            int cellNum = getCellNum(cellSegRef);

            int segId = getSegmentId(cellSegRef);

            String cellGroup = getCellGroup(cellSegRef);


            logger.logComment("Looking at file: "+ cellDataFiles[fileIndex].getName()
                              + " for cellGroup: "+ cellGroup
                              +", cellNum: "+cellNum
                              +", segId: "+segId
                              +", variable: "+variable
                              +", cellSegRef: "+ cellSegRef
                              +", PostSynapticObject; "+pso);

            if (segId>0)
            {
                onlySomaValues = false;
            }
            double conversionFactor = 1;


            String xUnit = "";
            String yUnit = "";


            if (variable.equals(SimPlot.VOLTAGE) || variable.contains(SimPlot.REV_POT))
            {
                conversionFactor = UnitConverter.getVoltage(conversionFactor,
                                                                    unitSystem,
                                                                    UnitConverter.NEUROCONSTRUCT_UNITS);

                yUnit = UnitConverter.voltageUnits[UnitConverter.NEUROCONSTRUCT_UNITS].getSymbol();
            }
            else if (variable.indexOf(SimPlot.SPIKE)>=0)
            {
                conversionFactor = 1;
            }
            else if (variable.indexOf(SimPlot.CURRENT) >= 0)
            {
                conversionFactor = UnitConverter.getCurrentDensity(conversionFactor,
                                                                           unitSystem,
                                                                           UnitConverter.NEUROCONSTRUCT_UNITS);

                yUnit = UnitConverter.currentUnits[UnitConverter.NEUROCONSTRUCT_UNITS].getSymbol();
            }
            else if (variable.indexOf(SimPlot.CONCENTRATION) >= 0)
            {
                conversionFactor = UnitConverter.getConcentration(conversionFactor,
                                                                          unitSystem,
                                                                          UnitConverter.NEUROCONSTRUCT_UNITS);
                //System.out.println("Conc conv factor: "+ conversionFactor);

                yUnit = UnitConverter.concentrationUnits[UnitConverter.NEUROCONSTRUCT_UNITS].getSymbol();
            }
            else if (variable.indexOf(SimPlot.COND_DENS) >= 0)
            {
                conversionFactor = UnitConverter.getConductanceDensity(conversionFactor,
                                                                               unitSystem,
                                                                               UnitConverter.NEUROCONSTRUCT_UNITS);

                yUnit = UnitConverter.conductanceDensityUnits[UnitConverter.NEUROCONSTRUCT_UNITS].getSymbol();
            }
            else if (variable.indexOf(SimPlot.SYN_COND) >= 0)
            {
                conversionFactor = UnitConverter.getConductance(conversionFactor,
                                                                               unitSystem,
                                                                               UnitConverter.NEUROCONSTRUCT_UNITS);

                yUnit = UnitConverter.conductanceUnits[UnitConverter.NEUROCONSTRUCT_UNITS].getSymbol();
            }

            if (yUnit.length()>0)
            {
                xUnit =  "ms";
            }
            
            logger.logComment("yUnit: "+yUnit+", xUnit: "+xUnit+", conversionFactor: "+conversionFactor);

            
            if (cellNum<0)  // probably from PyNN...
            {
                double[][] dataArrays = read2dDataFileToArrays(cellDataFiles[fileIndex], conversionFactor);
                
                for (int cellNumIndex= 0 ; cellNumIndex<dataArrays.length;cellNumIndex++)
                {
                    
                    DataStore ds = new DataStore(dataArrays[cellNumIndex], cellGroup, cellNumIndex, segId, variable, xUnit, yUnit, pso);

                    logger.logComment("Added a ds: "+ ds);
                    dataSources.add(ds);
                }
                //DataStore ds = new DataStore(dataArray, cellGroup, cellNum, segId, variable, xUnit, yUnit, pso);

                //dataSources.add(ds);

            }
            else if (variable.indexOf(SimPlot.SPIKE)<0)
            {
                double[] dataArray = readDataFileToArray(cellDataFiles[fileIndex], conversionFactor);
                DataStore ds = new DataStore(dataArray, cellGroup, cellNum, segId, variable, xUnit, yUnit, pso);

                dataSources.add(ds);

            }
            else
            {
                double[] spikeArray = readSpikesToArray(cellDataFiles[fileIndex], times, timeConversionFactor);
                DataStore ds = new DataStore(spikeArray, cellGroup, cellNum, segId, variable, xUnit, yUnit, pso);

                dataSources.add(ds);
            }

        }

        //GeneralUtils.timeCheck("Finished reading voltages");

        logger.logComment("There have been "+dataSources.size()+" data stores loaded:");

        for (DataStore ds: dataSources)
        {
            logger.logComment(ds.toString());
        }

        dataLoaded = true;
    }
    
    public boolean isDataLoaded()
    {
        return dataLoaded;
    }

/*
    public boolean dataOnlyForSoma()
    {
        return onlySomaValues;
    }*/


    public int getNumberTimeSteps()  throws SimulationDataException
    {
        if (!dataLoaded) throw new SimulationDataException("Data not yet loaded from files!");
        return times.length;
    }
/*
    public int getNumberVoltageSources()  throws SimulationDataException
    {
        if (!dataLoaded) throw new SimulationDataException("Data not yet loaded from files!");
        return this.getCellSegRefs(true).size();
    }*/


    public double getStartTime()  throws SimulationDataException
    {
        if (!dataLoaded) throw new SimulationDataException("Data not yet loaded from files!");
        return times[0];
    }


    public double getEndTime()  throws SimulationDataException
    {
        if (!dataLoaded) throw new SimulationDataException("Data not yet loaded from files!");
        return times[times.length-1];
    }

    public double getSimulationTime(int timeStep)  throws SimulationDataException
    {
        if (!dataLoaded) throw new SimulationDataException("Data not yet loaded from files!");
        return times[timeStep];
    }

    /**
     * Gets the references to the voltage source. Could be just a cell ref or a cell and segment reference
     * (i.e. CellRef_123 or CellRef_123.SegRef)
     */
    public ArrayList<String> getCellSegRefs(boolean voltageOnly)
    {
        ArrayList<String> refs = new ArrayList<String>();

        if (voltageOnly)
        {
            if (this.allVoltCellRefsCached == null)
            {
                for (DataStore ds : dataSources)
                {
                    if (ds.variable.equals(SimPlot.VOLTAGE))
                    {
                        refs.add(ds.getCellSegRef());
                    }
                }
                allVoltCellRefsCached = refs;
            }
            else
            {
                refs = allVoltCellRefsCached;
            }

        }
        else
        {
            if (this.allCellRefsCached == null)
            {
                for (DataStore ds : dataSources)
                {
                    refs.add(ds.getCellSegRef());
                }
                allCellRefsCached = refs;
            }
            else
            {
                refs = allCellRefsCached;
            }
        }
        //System.out.println("Refs: "+refs);
        return refs;
    }



    public double[] getVoltageAtAllTimes(String cellSegRef)  throws SimulationDataException
    {
        if (!dataLoaded) throw new SimulationDataException("Data not yet loaded from files!");

        ArrayList<DataStore> datas = this.getDataForCellSegRef(cellSegRef, false);

        DataStore volts = null;


        for (DataStore ds: datas)
        {
            String var = ds.getVariable();

            if (var.equals(SimPlot.VOLTAGE))
            {
                volts = getDataAtAllTimes(cellSegRef, SimPlot.VOLTAGE, true);
                return volts.dataPoints;
            }

            if (var.indexOf(SimPlot.SPIKE)>=0)
            {
                volts = getDataAtAllTimes(cellSegRef, var, true);
                return volts.dataPoints;
            }
        }
        throw new SimulationDataException("Problem finding voltage data in "+cellSegRef+"");


    }


    public DataStore getDataAtAllTimes(String cellItemRef,
                                       String variable,
                                       boolean incSpikeOrVoltage)  throws SimulationDataException
    {
        if (!dataLoaded) throw new SimulationDataException("Data not yet loaded from files!");

        String cellSegRef = getCellSegRef(cellItemRef);

        for (DataStore ds : dataSources)
        {
            if ( (ds.variable.equals(variable) ||
                  (incSpikeOrVoltage &&
                   (variable.indexOf(SimPlot.SPIKE) >= 0 || variable.equals(SimPlot.VOLTAGE)) &&
                   (ds.getVariable().indexOf(SimPlot.SPIKE) >= 0 || ds.getVariable().equals(SimPlot.VOLTAGE))))
                 && cellSegRefsEqual(cellSegRef, ds.getCellSegRef()))
            {
                return ds;
            }
        }

        
        throw new SimulationDataException("Problem loading data for "+variable+" in "+cellItemRef+". Data stores: "+ getCellSegRefs(false));
    }
    
    
    public DataSet getDataSet(String cellItemRef,
                              String variable,
                              boolean incSpikeOrVoltage)  throws SimulationDataException
    {
        if (!dataLoaded) throw new SimulationDataException("Data not yet loaded from files!");

        
        DataStore dataStore = getDataAtAllTimes(cellItemRef, variable, incSpikeOrVoltage);
        
        String ref = "Plot of "+ variable+" in "+ cellItemRef+ " ("+this.getSimulationName()+")";
        String desc = ref;
        DataSet dataSet = new DataSet(ref, desc, 
                                       "ms", 
                                       SimPlot.getUnits(variable),
                                       "Time",
                                       SimPlot.getLegend(variable));
        
        double[] points = dataStore.getDataPoints();

        for (int i = 0; i < times.length; i++)
        {
            dataSet.addPoint(times[i], points[i]);
            System.currentTimeMillis();
        }
        
        return dataSet;
        
    }


    public double[] getAllTimes()  throws SimulationDataException
    {
        if (!dataLoaded) throw new SimulationDataException("Data not yet loaded from files!");
        return times;
    }



    private double[] readDataFileToArray(File dataFile, double scaleFactor) throws SimulationDataException
    {
        String nextLine = null;
        double[] data = null;
        try
        {
            Reader in = new FileReader(dataFile);
            LineNumberReader reader = new LineNumberReader(in);

            /** @todo check if there's a quicker way to do this */

            //ArrayList<Double> tempList = new ArrayList<Double>(suggestedInitCapData);
            double[] tempArray = new double[suggestedInitCapData];

            int numDataPointsAdded = 0;

            while ( (nextLine = reader.readLine()) != null && nextLine.length()>0)
            {
                if (!nextLine.startsWith("//") && nextLine.trim().length()>0)
                {
                    double nextEntry = Double.parseDouble(nextLine);
                    if (tempArray.length<=numDataPointsAdded)
                    {
                        double[] tempArray2 = new double[numDataPointsAdded*2];
                        logger.logComment("Rescaling array of data points to size: "+ tempArray2.length);
                        for(int i=0;i<numDataPointsAdded;i++)
                        {
                            tempArray2[i] = tempArray[i];
                        }
                        tempArray = tempArray2;
                    }
                    tempArray[numDataPointsAdded] = scaleFactor * nextEntry;
                    numDataPointsAdded++;
                }
            }

            suggestedInitCapData = numDataPointsAdded; // for next time...

            in.close();
            
            if (tempArray.length == numDataPointsAdded)
            {
                //logger.logComment("Quick return", true);
                return tempArray;
            }
            
            data = new double[numDataPointsAdded];

            for (int i = 0; i < numDataPointsAdded; i++)
            {
                data[i] = tempArray[i];
            }
            //System.out.println("Read in "+data.length+" values. First: "+data[0]+", last: "+data[data.length-1] );
            return data;
        }
        catch (IOException e)
        {
            throw new SimulationDataException("Error reading from file: "+ dataFile.getAbsolutePath(), e);
        }
        catch (NumberFormatException ne)
        {
            throw new SimulationDataException("Error reading line: ("+nextLine+") from file: "+ dataFile.getAbsolutePath(), ne);
        }

    }

    /*
     * To handle data files saved through PyNN. These are 2 column data files with voltage & cell number, i.e.
     * -65      0
     * -65      1
     * -65.1    0
     * -65.1    1
     * ...
     * 
     */
    public static double[][] read2dDataFileToArrays(File dataFile, double scaleFactor) throws SimulationDataException
    {
        String nextLine = null;
        double[][] data = null;
        try
        {
            Reader in = new FileReader(dataFile);
            LineNumberReader reader = new LineNumberReader(in);

            /** @todo check if there's a quicker way to do this */
            
            Hashtable<Integer, ArrayList<Double>> tempList = new Hashtable<Integer, ArrayList<Double>>();

            while ( (nextLine = reader.readLine()) != null)
            {
                if (!nextLine.startsWith("//") && 
                    !nextLine.startsWith("#") && 
                    nextLine.trim().length()>0)
                {
                    String[] sp = nextLine.trim().split("\\s+");
                    if (sp.length!=2)
                        throw new SimulationDataException("Error reading PyNN format line: "
                                +nextLine+" in data file "+dataFile.getAbsolutePath());
                    
                    
                    double nextVal = Double.parseDouble(sp[0]);
                    int cellNum = (int)Float.parseFloat(sp[1]); // in case it's 0.0, 1.0 etc.
                    //logger.logComment(cellNum+": "+nextVal, true);
                    if (!tempList.containsKey(cellNum))
                        tempList.put(cellNum, new ArrayList<Double>(suggestedInitCapData));
                    ArrayList<Double> arr = tempList.get(cellNum);
                    arr.add(scaleFactor * nextVal);
                        
                }
            }

            if (tempList.size() > suggestedInitCapData) suggestedInitCapData = tempList.size();

            in.close();
            
            data = new double[tempList.keySet().size()][tempList.get(0).size()];

            Enumeration<Integer> cellNums = tempList.keys();
            
            while (cellNums.hasMoreElements())
            {
                int cellNum = cellNums.nextElement();
                ArrayList<Double> vals = tempList.get(cellNum);
                
                for (int i = 0; i < vals.size(); i++)
                {
                    data[cellNum][i] = vals.get(i);
                }
                ////////////data[i] = tempList.get(i);
            }
            //logger.logComment("Read in "+data[0].length+" values. First: "+data[0][0]+", last: "+data[data.length-1][data[0].length-1], true);
            return data;
        }
        catch (IOException e)
        {
            throw new SimulationDataException("Error reading from file: "+ dataFile.getAbsolutePath(), e);
        }
        catch (NumberFormatException ne)
        {
            throw new SimulationDataException("Error reading line: ("+nextLine+") from file: "+ dataFile.getAbsolutePath(), ne);
        }

    }



    public static double[] readSpikesToArray(File spikeFile, double[] times, double timeConversionFactor) throws SimulationDataException
    {
        logger.logComment("Reading spikes from file: "+ spikeFile);

        double nonSpikingVal = -100;
        double spikingVal = 100;


        String nextLine = null;
        double[] data = null;
        try
        {

            Reader in = new FileReader(spikeFile);
            LineNumberReader reader = new LineNumberReader(in);


            ArrayList<Double> spikeTimes = new ArrayList<Double>(suggestedInitCapSpikes);

            while ( (nextLine = reader.readLine()) != null && nextLine.length()>0)
            {
                if (!nextLine.startsWith("//") && nextLine.trim().length()>0)
                {
                    double nextEntry = Double.parseDouble(nextLine);
                    //logger.logComment("Found line: "+ nextEntry);
                    spikeTimes.add(nextEntry);
                }
            }

            in.close();

            if (suggestedInitCapSpikes< spikeTimes.size()) suggestedInitCapSpikes = spikeTimes.size();

            data = new double[times.length];
            int spikeCount = 0;

            boolean insideSpike = false;

            for (int timeStep = 0; timeStep < times.length; timeStep++)
            {
                double time = times[timeStep];

                if (spikeCount < spikeTimes.size() && time >= (spikeTimes.get(spikeCount) * timeConversionFactor))
                {
                    if (!insideSpike)
                    {
                        data[timeStep] = spikingVal;
                        spikeCount++;

                        insideSpike = true;
                    }
                    else
                    {
                        data[timeStep] = nonSpikingVal;
                        spikeCount++;
                    }
                }
                else
                {
                    data[timeStep] = nonSpikingVal;
                    insideSpike = false;
                }
            }


            //System.out.println("Read in "+data.length+" values. First: "+data[0]+", last: "+data[data.length-1] );
            return data;
        }
        catch (IOException e)
        {
            throw new SimulationDataException("Error reading from file: "+ spikeFile.getAbsolutePath(), e);
        }
        catch (NumberFormatException ne)
        {
            throw new SimulationDataException("Error reading line: ("+nextLine+") from file: "+ spikeFile.getAbsolutePath(), ne);
        }

    }



    public String getDateModified()
    {
        long timeModified = getTimesFile().lastModified();

        SimpleDateFormat formatter = new SimpleDateFormat("H:mm:ss (MMM d, yy)");
        java.util.Date modified = new java.util.Date(timeModified);


        return formatter.format(modified);

    }

    @Override
    public String toString()
    {
        return getSimulationName() + ": recorded at "+ getDateModified();
    }

    public String getSimulationName()
    {
        return dataDirectory.getName();
    }


    public File getCellPositionsFile()
    {
        return new File(dataDirectory.getAbsolutePath()
                        + System.getProperty("file.separator")
                        + POSITION_DATA_FILE);

    }


    public File getSimulationDirectory()
    {
        return dataDirectory;

    }


    public Properties getSimulationProperties()
    {
        return SimulationsInfo.getSimulationProperties(dataDirectory);
    }



    public File getNetConnectionsFile()
    {
        return new File(dataDirectory.getAbsolutePath()
                        + System.getProperty("file.separator")
                        + NETCONN_DATA_FILE);

    }



    public File getElecInputsFile()
    {
        return new File(dataDirectory.getAbsolutePath()
                        + System.getProperty("file.separator")
                        + ELEC_INPUT_DATA_FILE);

    }
    
    public ArrayList<DataStore> getAllLoadedDataStores()
    {
        return dataSources;
    }

    public ArrayList<DataStore> getDataForCellSegRef(String cellSegRef, boolean inclSynapses)
    {
        if (!dataLoaded) return null;

        ArrayList<DataStore> vars = new ArrayList<DataStore>();

        for (DataStore ds : dataSources)
        {
            logger.logComment("Checking getDataForCellSegRef: "+ds+" against "+ cellSegRef);

            if (cellSegRefsEqual(ds.getCellSegRef(), cellSegRef))
            {
                logger.logComment("Equals...: " );
                vars.add(ds);
            }
            if (inclSynapses && ds.getCellRef().equals(getCellOnlyReference(cellSegRef)))
            {
                if (ds.isSynapticMechData())
                {
                    logger.logComment("Found a syn mech...: " );
                    vars.add(ds);
                }
            }
        }
        return vars;
    }


    public ArrayList<String> getVariablesForAny()
    {
        if (!dataLoaded) return null;

        ArrayList<String> vars = new ArrayList<String>();

        for (DataStore ds : dataSources)
        {

            if (!vars.contains(ds.variable))
            {
                vars.add(ds.variable);
            }
        }
        return vars;
    }


    /**
     * Gets all the CellSegRefs relevant for that CellRefs, i.e. all recorded segments for that cell
     */
    public ArrayList<String> getCellSegRefsForCellRef(String cellRef)
    {
        if (!dataLoaded) return null;

        ArrayList<String> cellSegRefs = new ArrayList<String>();

        for (DataStore ds : dataSources)
        {
            logger.logComment("Checking getCellSegRefsForCellRef: "+ds+" against "+ cellRef);

            if (ds.getCellRef().equals(cellRef))
            {
                if (!cellSegRefs.contains(ds.getCellSegRef()))
                    cellSegRefs.add(ds.getCellSegRef());
            }
        }
        return cellSegRefs;
    }



    public ArrayList<String> getCellItemRefsForVar(String variable, boolean incSpikeOrVoltage, Hashtable<String, ArrayList<Integer>> cellsToUse)
    {
        if (!dataLoaded) return null;

        ArrayList<String> cellItemRefs = new ArrayList<String> ();
        Set<String> cgsToUse = cellsToUse.keySet();

        for (DataStore ds : dataSources)
        {
            String cg = ds.getCellGroupName();
            logger.logComment("Checking getCellItemRefsForVar: " + ds + " against " + variable+" for: "+ cg);

            if (cgsToUse.contains(cg) &&
                    (ds.getVariable().equals(variable) ||
                    (incSpikeOrVoltage &&
                 (variable.indexOf(SimPlot.SPIKE)>=0 || variable.equals(SimPlot.VOLTAGE))&&
                 (ds.getVariable().indexOf(SimPlot.SPIKE)>=0 || ds.getVariable().equals(SimPlot.VOLTAGE)))))
            {
                String ref = ds.getCellSegRef();
                ArrayList<Integer> cells = cellsToUse.get(cg);
                
                if(cells.contains(ds.getCellNumber()))
                {
                    if (ds.pso!=null)
                    {
                        ref = ref +"."+ ds.pso.getSynRef();
                    }

                    if (!cellItemRefs.contains(ref))
                        cellItemRefs.add(ref);
                }
            }
        }
        logger.logComment("cellItemRefs: "+cellItemRefs);

        return cellItemRefs;
    }





    public static String getCellRef(String cellGroupName, int cellNumber)
    {
        return cellGroupName + "_" + cellNumber;
    }


    public static String getCellSegRef(String cellGroupName, int cellNumber, int segId)
    {
        return getCellSegRef(cellGroupName, cellNumber, segId, false);
    }


    public boolean isOnlySomaValues()
    {
        return onlySomaValues;
    }

    public static boolean cellSegRefsEqual(String cellSegRefA, String cellSegRefB)
    {
        if (cellSegRefA.equals(cellSegRefB)) return true;

        if(cellSegRefA.endsWith(".0"))
        {
            if (cellSegRefB.equals(cellSegRefA.substring(0,cellSegRefA.length()-2)))
                return true;
        }
        if(cellSegRefB.endsWith(".0"))
        {
            if (cellSegRefA.equals(cellSegRefB.substring(0,cellSegRefB.length()-2)))
                return true;
        }

        return false;
    }


    public static String getCellSegRef(String cellGroupName, int cellNumber, int segId, boolean onlySomaValues)
    {
        if (segId<0) return cellGroupName + "_" + cellNumber;

        if (onlySomaValues)
        {
            return cellGroupName + "_" + cellNumber;
        }
        return cellGroupName + "_" + cellNumber+"."+segId;
    }


    public String getCellGroup(String cellRefOrCellSegRef)
    {
        if (cellRefOrCellSegRef.indexOf("_")<0)
            return cellRefOrCellSegRef;
        String cellRef = getCellRef(cellRefOrCellSegRef);
        
        if ((new File(dataDirectory, "PyNNUtils")).exists())
        {
            // A Pynn based sim
            return cellRef; // As all of the cell info will be in CellGroupA.dat etc.
        }

        return cellRef.substring(0, cellRef.lastIndexOf("_"));
    }

    private static String getCellRef(String cellRefOrCellSegRef)
    {
        String cellRef = null;

        if (cellRefOrCellSegRef.indexOf(".")>=0)
        {
            cellRef = cellRefOrCellSegRef.substring(0,cellRefOrCellSegRef.indexOf("."));
        }
        else
        {
            cellRef = cellRefOrCellSegRef;
        }
        //System.out.println("cellRef: "+cellRef);
        return cellRef;

    }


    public static int getCellNum(String cellRefOrCellSegRef)
    {
        String cellRef = getCellRef(cellRefOrCellSegRef);

        String num = cellRef.substring(cellRef.lastIndexOf("_") + 1);

        try
        {
            return Integer.parseInt(num);
        }
        catch (Exception ex) // could also be null...
        {
            return -1;
        }

    }




    /**
     * cellSegReference used to be of form SingleGranuleCell_0.gcdend1_2_seg (when individual segs
     * are recorded) or SingleGranuleCell_0 for only soma recorded
     */
    public static String getSegmentName(String cellSegReference)
    {
        if (cellSegReference.indexOf(".")<0)
        {
            return null;
        }
        else
        {
            return cellSegReference.substring(cellSegReference.indexOf(".") + 1);
        }
    }


    /**
     * Input: SampleCellGroup_0.0, output SampleCellGroup_0
     */
    public static String getCellOnlyReference(String cellSegReference)
    {
        if (cellSegReference.indexOf(".")<0) // shouldn't be
            return cellSegReference;

        return cellSegReference.substring(0, cellSegReference.indexOf("."));
    }



    /**
     * dataSourceName be in form:
     *     SampleCellGroup_0.0.NetConn_1.DoubExpSyn.6.COND for Syn obj
     */
    public static PostSynapticObject getPostSynapticObject(String dataSourceName)
    {
        String[] broken = dataSourceName.split("\\.");

        if (broken.length<5) return null;

        PostSynapticObject pso = new PostSynapticObject(broken[2],
                                                        broken[3],
                                                        getCellNum(broken[0]),
                                                        Integer.parseInt(broken[1]),
                                                        Integer.parseInt(broken[4]));

        return pso;
    }


    /**
     * dataSourceName be in form:
     *     SampleCellGroup_0.0 or
     *     SampleCellGroup_0.0.NaConductance_m or
     *     SampleCellGroup_0.NaConductance_m
     *     SampleCellGroup_0
     *     or
     *     SampleCellGroup_0.0.NetConn_1.DoubExpSyn.6.COND for Syn obj
     */
    public static String getVariable(String dataSourceName)
    {
        if (dataSourceName.indexOf(".")<0)
            return SimPlot.VOLTAGE;

        //String lastPart = dataSourceName.substring(dataSourceName.lastIndexOf(".")+1);

        String[] broken = dataSourceName.split("\\.");

        if (broken.length==2)
        {
            try
            {
                Integer.parseInt(broken[1]); // will succeed if dataSourceName is just SampleCellGroup_0.0
                return SimPlot.VOLTAGE;
            }
            catch (NumberFormatException ex)
            {
                return broken[1];
            }
        }

        return broken[broken.length-1];

    }

    public static boolean isSynDataSource(String dataSourceName)
    {
        return dataSourceName.split("\\.").length >3;
    }


    /**
     * dataSourceName be in form:
     *     SampleCellGroup_0.0 or
     *     SampleCellGroup_0.0.NaConductance_m or
     *     SampleCellGroup_0.NaConductance_m
     *     SampleCellGroup_0
     *     or
     *     SampleCellGroup_0.0.NetConn_1.DoubExpSyn.6.COND for Syn obj

     *
     * @return SampleCellGroup_0 or SampleCellGroup_0.0 depending on dataSourceName
     */
    public static String getCellSegRef(String dataSourceName)
    {
        if (dataSourceName.indexOf(".")<0) return dataSourceName;

        //String lastPart = dataSourceName.substring(dataSourceName.lastIndexOf(".")+1);


        String[] broken = dataSourceName.split("\\.");

        if (broken.length == 2)
        {
            try
            {
                Integer.parseInt(broken[1]); // will succeed if dataSourceName is just SampleCellGroup_0.0
                return dataSourceName;
            }
            catch (NumberFormatException ex) // will be thrown if dataSourceName is like SampleCellGroup_0.NaConductance_m
            {
                return dataSourceName.substring(0, dataSourceName.lastIndexOf("."));
            }
        }

        return broken[0]+"."+broken[1];


    }



    public static int getSegmentId(String cellSegReference)
    {
        if (cellSegReference.indexOf(".")<0)
            return -1;

        String id = cellSegReference.substring(cellSegReference.indexOf(".") + 1);

        if (id.indexOf(".") > 0)
        {
            id = id.substring(0, id.lastIndexOf("."));
        }

        try
        {
            return Integer.parseInt(id);
        }
        catch (NumberFormatException ex)
        {
            return -1;
        }

    }




    public static void main(String[] args)
    {
        //File f = new File("projects/PlotSave/simulations/Sim_43/");
        //File f = new File("projects/Spiky/simulations/Sim_33");
        File f = new File("nCexamples/Ex6_CerebellumDemo/simulations/Sim_51");
        SimulationData simulationData1 = null;
        try
        {
            //System.out.println("Info: "+ SimulationsInfo.getSimProps(f, false));
            simulationData1 = new SimulationData(f, true);
            simulationData1.initialise();

            ArrayList<DataStore> dss= simulationData1.getAllLoadedDataStores();
            for(DataStore ds: dss)
            {
                System.out.println(ds);
            }

            //System.out.println("dataOnlyForSoma: "+simulationData1.dataOnlyForSoma());
            System.out.println("Num time steps: "+simulationData1.getNumberTimeSteps());
            //System.out.println("Cell value: "+simulationData1.getVoltageAtTimeStep(33, "CellGroup_2_3.0"));

        }
        catch (SimulationDataException ex)
        {
            System.out.println("Error...");
            ex.printStackTrace();
        }

    }


    /**
     * Could do with a better name...
     */
    public class DataStore
    {
        private double[] dataPoints;

        private String cellGroupName = null;
        private int cellNumber = -1;
        private int segId = -1;
        private String variable = SimPlot.VOLTAGE;

        private String xUnit = "";
        private String yUnit = "";

        private double maxVal = -1* Double.MAX_VALUE;
        private double minVal = Double.MAX_VALUE;

        /**
         * Duplication of data here...
         */
        private PostSynapticObject pso = null;



        public DataStore(double[] dataPoints,
                         String cellGroupName,
                         int cellNumber,
                         int segId,
                         String variable,
                         String xUnit,
                         String yUnit,
                         PostSynapticObject pso)
        {
            this.dataPoints = dataPoints;
            this.cellGroupName = cellGroupName;
            this.cellNumber = cellNumber;
            this.segId = segId;
            this.variable = variable;
            this.xUnit = xUnit;
            this.yUnit = yUnit;
            this.pso = pso;

            for (int i = 0; i < dataPoints.length; i++)
            {
                if (dataPoints[i]>maxVal) maxVal = dataPoints[i];
                if (dataPoints[i]<minVal) minVal = dataPoints[i];
            }

        }

        public String getCellRef()
        {
            return SimulationData.getCellRef(cellGroupName, cellNumber);
        }

        public String getCellSegRef()
        {
            return SimulationData.getCellSegRef(cellGroupName, cellNumber, segId);
        }

        public String getCellGroupName()
        {
            return this.cellGroupName;
        }
        public String getVariable()
        {
            return this.variable;
        }

        public PostSynapticObject getPostSynapticObject()
        {
            return this.pso;
        }

        public boolean isSynapticMechData()
        {
            return pso != null;
        }




        public int getCellNumber()
        {
            return this.cellNumber;
        }

        public double[] getDataPoints()
        {
            return dataPoints;
        }


        public int getAssumedSegmentId()
        {
            if (segId<0) return 0;
            return segId;
        }


        public double getMaxVal()
        {
            return this.maxVal;
        }

        public double getMinVal()
        {
            return this.minVal;
        }




        public boolean isSegmentSpecified()
        {
            return segId>0;
        }



        public String getXUnit()
        {
            return this.xUnit;
        }

        public String getYUnit()
        {
            return this.yUnit;
        }

        @Override
        public String toString()
        {
            String synInfo = "";
            if (pso!=null) synInfo =  " (synapse: "+pso.getSynRef()+")";
            String info = "DataStore"+synInfo+" for "+variable+" on segment: "+segId+" on "+ getCellRef();

            info = info + ": ("+dataPoints[0]+", "+dataPoints[1]+", ... ["+dataPoints.length+" entries])";

            return info;
        }
    }


}
