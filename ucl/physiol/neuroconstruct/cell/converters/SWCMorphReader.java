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

package ucl.physiol.neuroconstruct.cell.converters;

import java.io.*;
import java.util.*;
import javax.vecmath.*;

import ucl.physiol.neuroconstruct.cell.*;
import ucl.physiol.neuroconstruct.cell.utils.*;
import ucl.physiol.neuroconstruct.utils.*;

/**
 *
 * A class for importing SWC morphology files (.swc files), and creating Cells
 * which can be used by the rest of the application
 *
 * @author Padraig Gleeson
 *  
 *
 */

public class SWCMorphReader extends FormatImporter
{
    private static ClassLogger logger = new ClassLogger("SWCMorphReader");
    

    private boolean daughtersOfSomaInherit = false;
    private boolean includeAnatFeatures = false;

    private boolean removeDuplicatedPoints = true;
    
    private int compTypeCutoff = 10;
    
    private int compTypeSoma = 1;
    private String somaSectionName = "Soma";
    private String somaExtraSecPrefix = "ExtraSomaSec_";

    private ArrayList<PointInfo> somaPoints = new ArrayList<PointInfo>();
    private ArrayList<PointInfo> otherPoints = new ArrayList<PointInfo>();

    // To mimic the cvapp/neurolucida colours..
    
    String somaColourGroup = "Colour_White";
    String dendColourGroup = "Colour_Green";
    String dendApicalColourGroup = "Colour_Magenta";
    String axonColourGroup = "Colour_DarkGrey";
    String c1ColourGroup = "Colour_Red";
    String c2ColourGroup = "Colour_Blue";
    String c3ColourGroup = "Colour_Yellow";


    public SWCMorphReader()
    {
        super("SWCMorphReader",
                            "Importer of SWC (Cvapp format) files",
                            new String[]{".swc"});

    }


    public Cell loadFromMorphologyFile(File morphologyFile, String name) throws MorphologyException
    {
        logger.logComment("Parsing file: " + morphologyFile);
        Cell cell = new Cell();
        cell.setInstanceName(name);

        try
        {
            Reader in = new FileReader(morphologyFile);
            BufferedReader lineReader = new BufferedReader(in);

            StringBuffer description = new StringBuffer("Cell morphology generated by neuroConstruct from file: "
                                                        + morphologyFile.getName()
                                                        + "\n");

            String nextLine = null;
            int lineCount = 0;


            //Point4f prevPoint = null;
            //String prevPointName = null;
            Hashtable<String, String> repeatedPointAliases = new Hashtable<String, String>();
            
            while ( (nextLine = lineReader.readLine()) != null)
            {
                lineCount++;
                //String origLine = new String(nextLine);
                nextLine = nextLine.trim();
                
                logger.logComment("Looking at line num " + lineCount + ": " + nextLine);


                if (nextLine.startsWith("#"))
                {
                    logger.logComment("Comment: " + nextLine);
                    if (nextLine.length()>2)
                    description.append(nextLine.substring("# ".length())+"\n");
                }
                else if (nextLine.length()==0)
                {
                    logger.logComment("Empty line...");
                }
                else
                {
                    String[] items = nextLine.split("\\s+");

                    if (items.length < 7)
                    {
                        String error = "Problem splitting up line (expecting at least 6 elements): " + nextLine;
                        throw new MorphologyException(morphologyFile.getAbsolutePath(), error);
                    }
                    else
                    {
                        String segmentName = cleanUpName(items[0]);
                        String parentName = cleanUpName(items[6]);
                        
                        //boolean hasParent = (!items[6].equals("-1"));
                        
                        float xCoord = Float.parseFloat(GeneralUtils.replaceAllTokens(items[2], ",", ""));
                        float yCoord = Float.parseFloat(GeneralUtils.replaceAllTokens(items[3], ",", ""));
                        float zCoord = Float.parseFloat(GeneralUtils.replaceAllTokens(items[4], ",", ""));
                        float radius = Float.parseFloat(GeneralUtils.replaceAllTokens(items[5], ",", ""));

                        Point3f p = new Point3f(xCoord, yCoord, zCoord);
                        //Point4f point = new Point4f(xCoord, yCoord, zCoord, radius);

                        int sectionType = -1;
                        
                        try
                        {
                            sectionType = (int)Float.parseFloat(items[1]);
                        }
                        catch (NumberFormatException e)
                        {
                            logger.logError("Error reading section type info from line", e);
                        }

                        logger.logComment("Line indicating type: "+ sectionType);

                        if (repeatedPointAliases.keySet().contains(parentName))
                        {
                            parentName = repeatedPointAliases.get(parentName);
                        }

                        boolean includePoint = true;
                        
                        if (removeDuplicatedPoints && parentName!=null)
                        {
                            boolean checkParent = true;
                            String parentToCheck = parentName;

                            while(checkParent)
                            {
                                PointInfo parInfo = getPointInfo(parentToCheck);
                                if (parInfo!=null && parInfo.xyz.equals(p) /*&& parInfo.radius==radius*/)
                                {
                                    //System.out.println("Improving parent of: "+segmentName+" at "+p+" from "+ parInfo);
                                    includePoint = false;
                                    repeatedPointAliases.remove(segmentName);
                                    repeatedPointAliases.put(segmentName, parentToCheck);
                                    parentToCheck = parInfo.parentName;
                                }
                                else
                                {
                                    checkParent = false;
                                }
                            }
                        }
                        if (includePoint)
                        {
                            //prevPoint = point;
                            //prevPointName = segmentName;
                            
                            if (sectionType==compTypeSoma)
                            {
                                PointInfo pi = new PointInfo(p,
                                        sectionType, radius, segmentName, parentName);

                                somaPoints.add(pi);

                            }
                            else if (includeAnatFeatures || sectionType<compTypeCutoff)
                            {
                                PointInfo pi = new PointInfo(p,
                                        sectionType, radius, segmentName, parentName);

                                otherPoints.add(pi);
                            }
                        }
                        
                    }
                }
            }
            if (lineCount == 0)
            {
                GuiUtils.showErrorMessage(logger, "Error. No lines found in file: " + morphologyFile, null, null);
            }

            logger.logComment("somaPoints: "+ somaPoints);
            logger.logComment("repeatedPointAliases: "+ repeatedPointAliases);
            
            
            Hashtable<String, Segment> origNamesVsSegments = new Hashtable<String, Segment>();

            String firstSegName = null;
            String firstSegEndpointName = null;
            Segment firstSeg = null;
            
            if (somaPoints.size()>0)
            {
                
                Section somaSec = null;
                
                PointInfo firstPi = somaPoints.get(0);
                
                somaSec = new Section(somaSectionName);
                
                firstSeg = cell.addFirstSomaSegment(firstPi.radius,
                        firstPi.radius,
                        cleanUpName(firstPi.name),
                        firstPi.xyz,
                        firstPi.xyz,
                        somaSec);
                
                firstSegName = (firstPi.name);
                
                addGroups(firstSeg, firstPi.type);
                
                origNamesVsSegments.put(firstPi.name, firstSeg);
                logger.logComment("firstSeg: "+ firstSeg);
                logger.logComment("firstSeg section: "+ firstSeg.getSection());
                
                ArrayList<String> takenParentOrigNames = new ArrayList<String>();
                
                for(int i=1;i<somaPoints.size();i++)
                {
                    PointInfo pi = somaPoints.get(i);
                    logger.logComment("---  next soma pi: "+ pi);
                    logger.logComment("takenParentOrigNames: "+ takenParentOrigNames);
                    
                    if (i==1)
                    {
                        firstSeg.setEndPointPositionX(pi.xyz.x);
                        firstSeg.setEndPointPositionY(pi.xyz.y);
                        firstSeg.setEndPointPositionZ(pi.xyz.z);
                        
                        firstSeg.setRadius(pi.radius);
                        firstSegEndpointName = pi.name;

                        logger.logComment("firstSeg now: "+ firstSeg);
                        logger.logComment("firstSeg section: "+ firstSeg.getSection());
                    }
                    else
                    {
                        Segment parSegment = null;

                        logger.logComment("pi.parentName: "+ pi.parentName);
                        logger.logComment("firstSeg section: "+ firstSeg.getSection());
                        
                        if(!takenParentOrigNames.contains(pi.parentName)) // check parent not already connected...
                        {
                            if (pi.parentName.equals(firstSegEndpointName))  
                            {
                                parSegment = firstSeg;
                                takenParentOrigNames.add(pi.parentName);
                                takenParentOrigNames.add(firstSegName);
                            }
                            else
                            {
                                parSegment = origNamesVsSegments.get(pi.parentName);
                                takenParentOrigNames.add(pi.parentName);
                            }
    
                            logger.logComment("parSegment: "+ parSegment);
                            
                            // Doesn't matter if this is add dend seg, parent's groups will determine soma/dend, etc.
                            Segment nextSeg = cell.addDendriticSegment(pi.radius,
                                    cleanUpName(pi.name),
                                    pi.xyz,
                                    parSegment,
                                    1,
                                    parSegment.getSection().getSectionName(),
                                    true);
    
                            logger.logComment("nextSeg: "+ nextSeg);
                            
                            origNamesVsSegments.put(pi.name, nextSeg);
                            
                        }
                        else // make a new section...
                        {
                            parSegment = origNamesVsSegments.get(pi.parentName);
                            
                            float connPoint = 1;

                            if (pi.parentName.equals(firstSegName))  connPoint = 0; 
                            
                            logger.logComment("parSegment for runt: "+ parSegment);
                            
                            String segName = cleanUpName(pi.name);
                            String secName = somaExtraSecPrefix + segName;
                            
                            Segment nextSeg = cell.addDendriticSegment(pi.radius,
                                    segName,
                                    pi.xyz,
                                    parSegment,
                                    connPoint,
                                    secName,
                                    true);
                            
                            nextSeg.getSection().addToGroup("ExtraSomaSegments");

                            nextSeg.getSection().addToGroup(somaColourGroup);
                            
                            nextSeg.setComment("Segment originally specified as in soma being put in seperate section, as it's parent segment already has a child in the soma section");
    
                            logger.logComment("nextSeg: "+ nextSeg);
                            
                            origNamesVsSegments.put(pi.name, nextSeg);
                        }
                        
                        logger.logComment("Soma segs: "+ cell.getOnlySomaSegments()+" \n \n");
                    }
                    firstSeg.getSection().addToGroup(Section.SOMA_GROUP); // to ensure...

                    logger.logComment("firstSeg section: "+ firstSeg.getSection());
                }
            }
            logger.logComment("otherPoints: "+ otherPoints);

            ArrayList<String> singleParents = new ArrayList<String>();
            ArrayList<String> doubleParents = new ArrayList<String>();
            
            for(PointInfo pi: otherPoints)
           {
                if (singleParents.contains(pi.parentName)) doubleParents.add(pi.parentName);
                singleParents.add(pi.parentName);
           }
            
            //String currentSectionName = null;
            
            for(PointInfo pi: otherPoints)
            {
                Segment parSegment = origNamesVsSegments.get(pi.parentName);
                
                if (pi.parentName.equals(firstSegEndpointName)) parSegment = firstSeg;

                logger.logComment("----      Adding point: "+pi+", parent: "+ parSegment);
                
                float connPoint = 1;

                if (pi.parentName.equals(firstSegName))  connPoint = 0; 
               
                
                String segName = cleanUpName(pi.name);

                String secName = null;
                //if ()
                
                boolean inherit = false;
                
                if (parSegment.isSomaSegment() || 
                        parSegment.getSection().getGroups().contains(somaColourGroup)) // for the extra soma segs
                {
                    inherit = this.daughtersOfSomaInherit;
                    secName = "Sec_"+ segName;
                    
                }
                else if (doubleParents.contains(pi.parentName)) // i.e. a split...
                {
                    secName = "Sec_"+ segName;
                }
                else
                {
                    secName = parSegment.getSection().getSectionName();
                }
                

                logger.logComment("secName: "+ secName);
                    
                
                Segment nextSeg = cell.addDendriticSegment(pi.radius,
                        segName,
                        pi.xyz,
                        parSegment,
                        connPoint,
                        secName,
                        inherit);
                
                addGroups(nextSeg, pi.type);
                
                logger.logComment("nextSeg: "+ nextSeg);
                
                origNamesVsSegments.put(pi.name, nextSeg);
            }

            cell.setCellDescription(description.toString());
        }
        catch (IOException e)
        {
            GuiUtils.showErrorMessage(logger, "Error: " + e.getMessage(), e, null);
            return null;
        }
        
        logger.logComment("Completed parsing of file: " + morphologyFile);

        return cell;
    }

    private PointInfo getPointInfo(String name)
    {
        for(int i=(otherPoints.size()-1);i>=0;i--) // backwards...
        {
            if (otherPoints.get(i).name.equals(name))
                return otherPoints.get(i);
        }
        for(int i=(somaPoints.size()-1);i>=0;i--) // backwards...
        {
            if (somaPoints.get(i).name.equals(name))
                return somaPoints.get(i);
        }
        return null;
    }
    
    
    private void addGroups(Segment newSeg, int sectionType)
    {
        switch (sectionType)
        {
            case 0:
                break;
            case 1:
                newSeg.getSection().addToGroup(Section.SOMA_GROUP);
                newSeg.getSection().addToGroup(somaColourGroup);
                break;
            case 2:
                newSeg.getSection().addToGroup(Section.AXONAL_GROUP);
                newSeg.getSection().addToGroup(axonColourGroup);
                
                break;
            case 3:
                newSeg.getSection().addToGroup(Section.DENDRITIC_GROUP);
                newSeg.getSection().addToGroup(dendColourGroup);
                break;
            case 4:
                newSeg.getSection().addToGroup("apical_dendrite");
                newSeg.getSection().addToGroup(dendApicalColourGroup);
                break;
            case 5:
                newSeg.getSection().addToGroup("custom-1");
                newSeg.getSection().addToGroup(c1ColourGroup);
                break;
            case 6:
                newSeg.getSection().addToGroup("custom-2");
                newSeg.getSection().addToGroup(c2ColourGroup);
                break;
            case 7:
                newSeg.getSection().addToGroup("custom-n");
                newSeg.getSection().addToGroup(c3ColourGroup);
                break;
            default:
                    newSeg.getSection().addToGroup("undefined");
            break;

        }
    }
    

    public void daughtersInherit(boolean adj)
    {
        daughtersOfSomaInherit = adj;
    }

    public void includeAnatFeatures(boolean adj)
    {
        includeAnatFeatures = adj;
    }

    
    
    private static String cleanUpName(String secNameInFile)
    {
       try
       {
           Float.parseFloat(secNameInFile);
           return "Comp_"+secNameInFile;
       }
       catch(NumberFormatException nfe)
       {
           
       }
       return secNameInFile;
       
    }
    
    
    private class PointInfo
    {
        protected Point3f xyz = null;
        protected int type = -1;
        protected float radius = -1;
        protected String name = null;
        protected String parentName = null;
        
        public PointInfo(Point3f xyz, 
                          int type,
                          float radius,
                          String name,
                          String parentName)
        {
            this.xyz = xyz;
            this.type = type;
            this.radius = radius;
            this.name = name;
            this.parentName = parentName;
        }
        
        @Override
        public String toString()
        {
            return "PointInfo: "+ name+"("+parentName+")"
            +xyz+", R: "+radius+"("+type+")\n";
        }
    }

    public static void main(String[] args)
    {

        try
        {
            //File f = (new File("../temp/l22s.swc")).getCanonicalFile();
            File f = (new File("../temp/enthoCNG.swc")).getCanonicalFile();
            //File f = (new File("../temp/c73162.CNG.swc")).getCanonicalFile();
            
            logger.logComment("loading cell...");
            GeneralUtils.timeCheck("Before loading swc");


            SWCMorphReader swcReader = new SWCMorphReader();

            Cell swcCell = swcReader.loadFromMorphologyFile(f, "SWCCellll");


            GeneralUtils.timeCheck("After loading swc");

            System.out.println(CellTopologyHelper.getValidityStatus(swcCell));
            
            if (true) return;

            System.out.println(CellTopologyHelper.printDetails(swcCell, null));

            GeneralUtils.timeCheck("After showing cell info...");

            File cellFile = new File("c:\\temp\\temp.xml");

            MorphMLConverter.saveCellInJavaXMLFormat(swcCell, cellFile);

            GeneralUtils.timeCheck("After saving as XML...");

            Cell cellGenerated = MorphMLConverter.loadFromJavaXMLFile(cellFile);

            GeneralUtils.timeCheck("After loading from XML...");

            logger.logComment(CellTopologyHelper.printShortDetails(cellGenerated));

            GeneralUtils.timeCheck("After showing cell info...");




        }
        catch (Exception ex)
        {
            ex.printStackTrace();
        }
    }

}
