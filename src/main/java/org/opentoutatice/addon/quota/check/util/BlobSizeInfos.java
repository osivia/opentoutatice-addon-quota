package org.opentoutatice.addon.quota.check.util;


public class BlobSizeInfos {
    
    public BlobSizeInfos(long number, long size) {
        super();
        this.number = number;
        this.size = size;
    }

    private long number;
    private long size;
    
    /**
     * Getter for number.
     * @return the number
     */
    public long getNumber() {
        return number;
    }
    

    /**
     * Getter for size.
     * @return the size
     */
    public long getSize() {
        return size;
    }
    


}
