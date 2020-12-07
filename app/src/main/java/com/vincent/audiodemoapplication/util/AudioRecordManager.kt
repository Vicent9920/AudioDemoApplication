package com.vincent.audiodemoapplication.util

import android.media.AudioFormat
import android.media.AudioRecord
import android.media.MediaRecorder
import java.io.*

class AudioRecordManager {
    // 录音实现
    private var recorder: AudioRecord? = null
    // 录音状态 0 开始录音 -1 暂停录音 1 录音结束
    private var status = 1
    private var recordFile:File? = null
    private var wavFile: RandomAccessFile? = null
     fun startRecording(file: File) {
         status = 0
         recordFile = file
        val buffer = AudioRecord.getMinBufferSize(48000,
                AudioFormat.CHANNEL_IN_STEREO,
                AudioFormat.ENCODING_PCM_16BIT)
        recorder = AudioRecord(MediaRecorder.AudioSource.MIC,
                48000,
                AudioFormat.CHANNEL_IN_STEREO,
                AudioFormat.ENCODING_PCM_16BIT,
                buffer).apply {
            startRecording()
        }
        Thread(Runnable {
            wavFile = randomAccessFile(file)
            val outputStream = createStream(file)
//            outputStream?.write(ByteArray(44))
//            wavFile?.seek(44)
            while (status!= 1){
                while (status == 0) {
                    val array = ByteArray(buffer)
                    if (AudioRecord.ERROR_INVALID_OPERATION != recorder!!.read(array, 0, buffer)) {
                        outputStream?.write(array)
                    }
                }
            }
            outputStream?.close()
        }).start()

    }

    fun pauseRecording(){
        status = -1
    }

     fun stopRecording() {
         recorder?.apply {
             stop()
             release()
         }
         recorder = null
         wavFile?.let { writeWavHeader(it) }
         status = 1
     }



    /**
     * 生成RandomAccessFile
     */
    private fun randomAccessFile(file: File): RandomAccessFile? {
        val randomAccessFile: RandomAccessFile
        randomAccessFile = try {
            RandomAccessFile(file, "rw")
        } catch (e: FileNotFoundException) {
            throw RuntimeException(e)
        }
        return randomAccessFile
    }

    /***
     * 写入 wavHeader
     */
    private fun writeWavHeader(wavFile: RandomAccessFile) {
        val totalAudioLen: Long = FileInputStream(recordFile).channel.size()
        try {
            wavFile.seek(0) // to the beginning
            wavFile.write(wavFileHeader(totalAudioLen, totalAudioLen + 36, 48000, 2,
                    16 * 48000L * 2 / 8, 16))
            wavFile.close()
        } catch (e: IOException) {
            e.printStackTrace()
        }
    }

    /**
     * 生成 WavHeader
     * @param totalAudioLen 音频文件长度
     * @param totalDataLen 总的文件长度
     * @param longSampleRate 采样率
     * @param channels 音频通道的配置
     * @param byteRate 字节速度
     * @param bitsPerSample 字节采样率
     */
    private fun wavFileHeader(totalAudioLen: Long, totalDataLen: Long, longSampleRate: Long,
                              channels: Int, byteRate: Long, bitsPerSample: Byte): ByteArray? {
        val header = ByteArray(44)
        header[0] = 'R'.toByte() // RIFF/WAVE header
        header[1] = 'I'.toByte()
        header[2] = 'F'.toByte()
        header[3] = 'F'.toByte()
        header[4] = (totalDataLen and 0xff).toByte()
        header[5] = (totalDataLen shr 8 and 0xff).toByte()
        header[6] = (totalDataLen shr 16 and 0xff).toByte()
        header[7] = (totalDataLen shr 24 and 0xff).toByte()
        header[8] = 'W'.toByte()
        header[9] = 'A'.toByte()
        header[10] = 'V'.toByte()
        header[11] = 'E'.toByte()
        header[12] = 'f'.toByte()// 'fmt ' chunk
        header[13] = 'm'.toByte()
        header[14] = 't'.toByte()
        header[15] = ' '.toByte()
        header[16] = 16 // 4 bytes: size of 'fmt ' chunk
        header[17] = 0
        header[18] = 0
        header[19] = 0
        header[20] = 1 // format = 1
        header[21] = 0
        header[22] = channels.toByte()
        header[23] = 0
        header[24] = (longSampleRate and 0xff).toByte()
        header[25] = (longSampleRate shr 8 and 0xff).toByte()
        header[26] = (longSampleRate shr 16 and 0xff).toByte()
        header[27] = (longSampleRate shr 24 and 0xff).toByte()
        header[28] = (byteRate and 0xff).toByte()
        header[29] = (byteRate shr 8 and 0xff).toByte()
        header[30] = (byteRate shr 16 and 0xff).toByte()
        header[31] = (byteRate shr 24 and 0xff).toByte()
        header[32] = (channels * (bitsPerSample / 8)).toByte() //
        // block align
        header[33] = 0
        header[34] = bitsPerSample // bits per sample
        header[35] = 0
        header[36] = 'd'.toByte()
        header[37] = 'a'.toByte()
        header[38] = 't'.toByte()
        header[39] = 'a'.toByte()
        header[40] = (totalAudioLen and 0xff).toByte()
        header[41] = (totalAudioLen shr 8 and 0xff).toByte()
        header[42] = (totalAudioLen shr 16 and 0xff).toByte()
        header[43] = (totalAudioLen shr 24 and 0xff).toByte()
        return header
    }

    /**
     * 生成OutputStream
     */
    private fun createStream(file: File?): OutputStream? {
        if (file == null) throw java.lang.RuntimeException("file is null !")
        val outputStream: OutputStream
        outputStream = try {
            FileOutputStream(file)
        } catch (e: FileNotFoundException) {
            throw java.lang.RuntimeException("could not build OutputStream from" +
                    " this file" + file.name, e)
        }
        return outputStream
    }
}