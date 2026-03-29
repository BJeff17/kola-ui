sed -i '/private int scrollY;/a \    private int scrollX;\n    private int contentWidth;' src/components/ScrollView.java
sed -i 's/this.contentHeight = Math.max(getHeight(), contentHeight);/this.contentHeight = Math.max(getHeight(), contentHeight);/g' src/components/ScrollView.java
