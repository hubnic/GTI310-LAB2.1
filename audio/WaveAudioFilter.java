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
			addEcho();
		}else{
			JOptionPane.showMessageDialog(null, "Désolé, le fichier importé n'est pas valide");
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
		//System.out.println(fileFormat + " " + fileRate + " " + chunkSize);
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
		
		int microsec = 1;
		int sampleSize = chunkSize/*44100(8bits) ou 88200(16bits)*/  *(microsec*1000);  // 88.2*nb de microsec de delais pour du 16 bit, 44.1*nb de delais pour du 8 bit
		double attenuation = 0;
		int newDataSubChunkSize = (int)(dataChunkSize + sampleSize);
		newDataSubChunk = new byte[newDataSubChunkSize];
		byte[] echoTable = new byte[sampleSize];
		
		for(int i=0;i<dataChunkSize/sampleSize + 1;i++){
			dataSubChunk = fileSource.pop(sampleSize);
			
			insertDataToNewTable(dataSubChunk, echoTable, i*sampleSize);
			echoTable = dataSubChunk;
		}

		newFile.push(modifyHeader());
		//push value
		newFile.push(newDataSubChunk);
		newFile.close();
	}
	
	private void insertDataToNewTable(byte[] tableIn, byte[] tableEcho, int index){
		//System.out.println();	
		for(int i = 0; i < tableIn.length; i++){
			if(index != 0){
				newDataSubChunk[i+index] = (byte) (tableIn[i] & 0xFF | (tableEcho[i] & 0xFF) << 8);
				//System.out.println(tableIn[i] + " " + (byte) (tableIn[i] & 0xFF | (tableEcho[i] & 0xFF) << 8));
			}
			else{
				newDataSubChunk[i] = tableIn[i];
			}
		}
		
	}
	
	private byte[] modifyHeader(){
		byte[] newHeader = new byte[44];
		byte[] newFileSize = new byte[4];
		byte[] newDataChunkSize = new byte[4];
		
		newHeader = riffChunk;
		fileSource.close();
		newFileSize = intToByteArray(newDataSubChunk.length + 44);
		newDataChunkSize = intToByteArray(newDataSubChunk.length);
		
		newHeader[4] = newFileSize[0];
		newHeader[5] = newFileSize[1];
		newHeader[6] = newFileSize[2];
		newHeader[7] = newFileSize[3];
		
		newHeader[40] = newDataChunkSize[0];
		newHeader[41] = newDataChunkSize[1];
		newHeader[42] = newDataChunkSize[2];
		newHeader[43] = newDataChunkSize[3];

		return newHeader;
	}
	//http://stackoverflow.com/questions/7619058/convert-a-byte-array-to-integer-in-java-and-vise-versa
	public byte[] intToByteArray(int value) {
		return new byte[] {
			(byte)value,
			(byte)(value >> 8),
			(byte)(value >> 16),
			(byte)(value >> 24)};
	}
}
