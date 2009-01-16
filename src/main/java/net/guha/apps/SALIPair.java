package net.guha.apps;

/**
 * @cdk.author Rajarshi Guha
 * @cdk.svnrev $Revision: 9162 $
 */
public class SALIPair implements Comparable {
    private String head;
    private String tail;
    private double tailActivity;
    private double headActivity;
    private double sali;
    private double sim;


    public SALIPair(String head, String tail, double headActivity, double tailActivity, double sali, double sim) {
        this.head = head;
        this.tail = tail;
        this.headActivity = headActivity;
        this.tailActivity = tailActivity;
        this.sali = sali;
        this.sim = sim;
    }

    public String getHead() {
        return head;
    }

    public void setHead(String head) {
        this.head = head;
    }

    public String getTail() {
        return tail;
    }

    public void setTail(String tail) {
        this.tail = tail;
    }

    public double getHeadActivity() {
        return headActivity;
    }

    public void setHeadActivity(double headActivity) {
        this.headActivity = headActivity;
    }

    public double getTailActivity() {
        return tailActivity;
    }

    public void setTailActivity(double tailActivity) {
        this.tailActivity = tailActivity;
    }

    public double getSali() {
        return sali;
    }

    public void setSali(double sali) {
        this.sali = sali;
    }

    public double getSimilarity() {
        return sim;
    }

    public int compareTo(Object o) throws ClassCastException {
        if (!(o instanceof SALIPair)) throw new ClassCastException("Expected a SALIPair object");
        SALIPair otherPair = (SALIPair) o;
        double otherSali = otherPair.getSali();
        if (this.sali > otherSali) return 1;
        else if (this.sali < otherSali) return -1;
        return 0;
    }
}
