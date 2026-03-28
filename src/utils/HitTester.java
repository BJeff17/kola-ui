package utils;

import main.BaseComp;

public class HitTester {

    /**
     * 
     * Vérifie si un point (x,y) est à l'intérieur des bornes d'un composant.
     * 
     * On utilise des comparaisons simples pour la performance.
     * 
     */

    public boolean isHit(int x, int y, BaseComp c) {
        return c != null && c.containsGlobalPoint(x, y);
    }

    /**
     * 
     * Recherche récursive du composant le plus profond sous le curseur.
     * 
     */

    public BaseComp findBaseComp(int x, int y, BaseComp root) {
        if (root == null || !isHit(x, y, root)) {
            return null;
        }

        BaseComp[] children = root.getChildren();
        if (children != null && children.length > 0) {
            for (int i = children.length - 1; i >= 0; i--) {
                BaseComp child = children[i];
                if (child == null)
                    continue;

                BaseComp target = findBaseComp(x, y, child);
                if (target != null) {
                    return target;
                }
            }
        }
        return root;
    }

}