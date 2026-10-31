package net.lukario.circleofinevitability.blasphemy_slate.beyonder_characteristics.omniscient_eye;

/** Sequence numbers run backwards: zero is strongest. Pure rules shared by server and tests. */
public final class StudyRules {
    private StudyRules() {}
    public static double gain(int observer, int ability) {
        if (observer < 0 || ability > 9 || ability < observer) return 0;
        return Math.min(100, 5.0 * Math.pow(2, ability - observer) / (10 - ability));
    }
    public static int compare(boolean favoriteA, double studyA, int sequenceA, String nameA,
                              boolean favoriteB, double studyB, int sequenceB, String nameB) {
        int c = Boolean.compare(favoriteB, favoriteA);
        if (c == 0) c = Double.compare(studyB, studyA);
        if (c == 0) c = Integer.compare(sequenceA, sequenceB);
        return c == 0 ? nameA.compareToIgnoreCase(nameB) : c;
    }
}
