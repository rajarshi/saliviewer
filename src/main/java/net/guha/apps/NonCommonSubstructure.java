package net.guha.apps;

import org.openscience.cdk.DefaultChemObjectBuilder;
import org.openscience.cdk.exception.CDKException;
import org.openscience.cdk.interfaces.IAtomContainer;
import org.openscience.cdk.isomorphism.UniversalIsomorphismTester;
import org.openscience.cdk.isomorphism.mcss.RMap;

import java.util.*;

/**
 * @cdk.author Rajarshi Guha
 * @cdk.svnrev $Revision: 9162 $
 */
public class NonCommonSubstructure {
    
    public static IAtomContainer getMCSS(IAtomContainer molecule1, IAtomContainer molecule2) throws CDKException {

        List mcsslist = UniversalIsomorphismTester.getOverlaps(molecule1, molecule2);
        int maxmcss = -9999999;
        IAtomContainer maxac = null;
        for (int i = 0; i < mcsslist.size(); i++) {
            IAtomContainer atomContainer = (IAtomContainer) mcsslist.get(i);
            if (atomContainer.getAtomCount() > maxmcss) {
                maxmcss = atomContainer.getAtomCount();
                maxac = atomContainer;
            }
        }
        return maxac;
    }

    public static IAtomContainer getNonCommonSubstructure(IAtomContainer atomContainer, IAtomContainer mcss) throws CDKException {

        // find all subgraphs of the original molecule matching the fragment
        List l = UniversalIsomorphismTester.getSubgraphMaps(atomContainer, mcss);

        Vector idlist = new Vector();

        // get the ID's (corresponding to the serial number of the Bond object in
        // the AtomContainer for the supplied molecule) of the matching bonds
        // (there will be repeats)
        for (int i2 = 0; i2 < l.size(); i2++) {
            Object aL = l.get(i2);
            List maplist = (List) aL;
            for (int i1 = 0; i1 < maplist.size(); i1++) {
                RMap i = (RMap) maplist.get(i1);
                idlist.add(new Integer(i.getId1()));
            }
        }

        HashSet hs = new HashSet(idlist);
        IAtomContainer noncommon = DefaultChemObjectBuilder.getInstance().newAtomContainer();

        for (int i = 0; i < atomContainer.getBondCount(); i++) {
            boolean inMCSS = false;
            for (Iterator iterator = hs.iterator(); iterator.hasNext();) {
                Integer id = (Integer) iterator.next();
                if (id.intValue() == i) {
                    inMCSS = true;
                    break;
                }
            }
            if (inMCSS) continue;
            noncommon.addBond(atomContainer.getBond(i));
        }
        return noncommon;
    }
}
