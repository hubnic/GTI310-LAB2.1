package audio;

import io.FileSink;
import io.FileSource;

import java.io.BufferedOutputStream;
import java.io.DataOutputStream;
import java.io.FileNotFoundException;
import java.io.FileOutputStream;
import java.io.UnsupportedEncodingException;

import javax.swing.JOptionPane;

public class WaveAudioFilter implements AudioFilter{

	/**
	 * @param args
	 */
	private String fileName = (this.getClass().getClassLoader().getResource("").getPath() + "medias-TP2\\App1Test1Mono8bits.wav").substring(1).replace('/', '\\');
	private String fileNameSink = (this.getClass().getClassLoader().getResource("").getPath() + "medias-TP2\\App1Test1Mono8bits2.wav").substring(1).replace('/', '\\');
	private byte[] riffChunk = new byte[44];
	private byte[] dataSubChunk;
	private byte[] newDataSubChunk;
	private FileSource fileSource;
	private FileSink newFile;
	private int dataChunkSize;
	private int chunkSize;
	
	public void process() 
	{
		try {
			newFile = new FileSink(fileNameSink);
	    }
	    catch (FileNotFoundException e) {
	    }
		
		getSource();
		if (verifyHeaders()){
			//System.out.println("True");
			addEcho();
		}else{
			JOptionPane.showMessageDialog(null, "Désolé, le fichier importé n'est pas un .wav (ou à 44100hz).");
		}
	}

	private boolean verifyHeaders()
	{
		riffChunk = fileSource.pop(44);
		byte[] bytes = new byte[4];
		String fileFormat = null;
		
		try {
			fileFormat = new String(bytes, "UTF-8");
		} catch (UnsupportedEncodingException e) {e.printStackTrace();}
		dataChunkSize = (riffChunk[40] & 0xFF | (riffChunk[41] & 0xFF) << 8 | (riffChunk[42] & 0xFF) << 16 | (riffChunk[43] & 0xFF) << 24);
		int fileRate = (riffChunk[24] & 0xFF | (riffChunk[25] & 0xFF) << 8 | (riffChunk[26] & 0xFF) << 16 | (riffChunk[27] & 0xFF) << 24);
		chunkSize = (riffChunk[34] & 0xFF | (riffChunk[35] & 0xFF) << 8);
		System.out.println(fileFormat + " " + fileRate + " " + chunkSize);
		if(fileFormat != "WAVE" && fileRate != 44100){
			fileSource.close();
			return false;
		}else{
			return true;	
		}
	}
	
	private void getSource()
	{
		try {
			fileSource = new FileSource(fileName);
			
		} catch (FileNotFoundException e) {
			System.out.println("Fichier non trouvé");
			e.printStackTrace();
		}
	}
	
	private void addEcho(){
		
		int microsec = 0;
		int dataChunkSize = chunkSize/*44100(8bits) ou 88200(16bits)*/  *(microsec*1000);  // 88.2*nb de microsec de delais pour du 16 bit, 44.1*nb de delais pour du 8 bit
		dataSubChunk = fileSource.pop(dataChunkSize);
		double indexSize = 44100/8000;
		int index = 0;
		double attenuation = 0;
		int newDataSubChunkSize = (int)(dataSubChunk.length + dataChunkSize);
		newDataSubChunk = new byte[newDataSubChunkSize];

		for(int i=0;i<dataSubChunk.length;i++){
			if(i == (int)(index * indexSize)){
				if(index>0){
					//newDataSubChunk[index] = (findYValue(i, ((int)index * indexSize))) + findYValue(i, ((int)index-1 * indexSize));
				}
				
				index++;
			}
		}

		newFile.push(modifyHeader());
		//push value
		newFile.push(newDataSubChunk);
		newFile.close();
	}
	
	private byte findYValue(int currentIndex, double targetedIndex){
		if(currentIndex < dataSubChunk.length -1){
			int value1 = dataSubChunk[currentIndex];
			int value2 = dataSubChunk[currentIndex + 1];
			
			int variation = (value2 - value1);
			return (byte)((int)((variation * (targetedIndex-value1)) + value1));
		}
		else
			return dataSubChunk[currentIndex];
	}
	private byte[] modifyHeader(){
		byte[] newHeader = new byte[36];
		byte[] newFileSize = new byte[4];
		byte[] fileRate = new byte[4];
		byte[] newDataChunkSize = new byte[4];
		byte[] newByteRate = new byte[4];
		
		newHeader = riffChunk;
		fileSource.close();
		newFileSize = intToByteArray(newDataSubChunk.length + 36);
		fileRate = intToByteArray(8000);
		newByteRate = intToByteArray(1000);
		newDataChunkSize = intToByteArray(newDataSubChunk.length);
		
		newHeader[4] = newFileSize[0];
		newHeader[5] = newFileSize[1];
		newHeader[6] = newFileSize[2];
		newHeader[7] = newFileSize[3];
		
		newHeader[24] = fileRate[0];
		newHeader[25] = fileRate[1];
		newHeader[26] = fileRate[2];
		newHeader[27] = fileRate[3];
		
		newHeader[28] = newByteRate[0];
		newHeader[29] = newByteRate[1];
		newHeader[30] = newByteRate[2];
		newHeader[31] = newByteRate[3];
		
		newHeader[40] = newDataChunkSize[0];
		newHeader[41] = newDataChunkSize[1];
		newHeader[42] = newDataChunkSize[2];
		newHeader[43] = newDataChunkSize[3];

		return newHeader;
	}
	
	public byte[] intToByteArray(int value) {
		return new byte[] {
			(byte)value,
			(byte)(value >> 8),
			(byte)(value >> 16),
			(byte)(value >> 24)};
	}
	
	//TODO À enlever?
	public static int byteArrayToInt(byte[] b) 
	{
	    int value = 0;
	    for (int i = 0; i < 4; i++) {
	        int shift = (4 - 1 - i) * 8;
	        value += (b[i] & 0x000000FF) << shift;
	    }
	    return value;
	}

}
