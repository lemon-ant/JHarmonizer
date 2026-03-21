class Gamma{int gammaLast;int gammaFirst;}

/* @jharmonizer:fully-off */
class Beta {
    int betaLast;   int betaFirst;
}

class Alpha {
    int walrus;
    int aardvark;

    // @jharmonizer:sort-off
    static class Inner {
        int zebra;
        int ant;

        // @jharmonizer:fully-off
        static class DeepInner{int later;  int earlier;}
    }
}
