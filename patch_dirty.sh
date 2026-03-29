sed -i '/public void render/i \    public Rectangle getDirtyRegion() {\n        return dirtyRegion;\n    }' src/utils/DirtyManager.java
