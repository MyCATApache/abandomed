import java.io.RandomAccessFile;
import java.nio.MappedByteBuffer;
import java.nio.channels.FileChannel;
import java.util.Arrays;

public class Test {
	   public void testDevZeroMap() throws Exception {
	        RandomAccessFile raf = new RandomAccessFile("/dev/zero", "rw");
	        try {
	        	int totalSize=65536;
	    		for(int i=0;i<totalSize;i++)
	    		{
	    			raf.write(0);
	    		}
	            MappedByteBuffer mbb = raf.getChannel().map(FileChannel.MapMode.READ_ONLY, 0, 65536);
	            // Create an array initialized to all "(byte) 1"
	            byte[] buf1 = new byte[totalSize];
	            Arrays.fill(buf1, (byte) 1);
	            // Read from mapped /dev/zero, and overwrite this array.
	            mbb.get(buf1);
	            // Verify that everything is zero
	            for (int i = 0; i < totalSize; i++) {
	                if((byte) 0!= buf1[i]){
	                	System.out.println("erro char");
	                }
	            }
	        } finally {
	            raf.close();
	        }
	    }
	public static void main(String[] args) throws Exception {
		new Test().testDevZeroMap();

	}

}
