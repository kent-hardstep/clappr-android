package io.clappr.player.playback

import android.net.Uri
import android.util.Log
import com.google.android.exoplayer2.C
import com.google.android.exoplayer2.drm.ExoMediaDrm
import com.google.android.exoplayer2.drm.MediaDrmCallback
import com.google.android.exoplayer2.upstream.DataSourceInputStream
import com.google.android.exoplayer2.upstream.DataSpec
import com.google.android.exoplayer2.upstream.HttpDataSource
import com.google.android.exoplayer2.util.Util
import java.io.FileNotFoundException
import java.io.IOException
import java.util.*

/**
 * Created by Jango on 21/05/17.
 *
 * This class serves the purpose of @MediaDrmCallback for Widevine.
 * This class is a sample representation of implementation of MediaDrmCallback which can be
 * customized as per the requirement.
 * By customization we mean, if Your license server needs some authorization parameters you can create
 * here and pass them in proper header keys in @executeKeyRequest method.
 */

class CustomDrmCallback
/**
 * This is a typical generic constructor which takes HttpDataSource.Factory for Network calls
 * and var objects for custom implementation.
 * Other constructor can be made to facilitate custom implmentation
 * @param dataSourceFactory
 * @param licUrl Url for License Server to acquire license
 * @param obj
 */
(private val dataSourceFactory: HttpDataSource.Factory, private val licUrl: String, vararg obj: Any) : MediaDrmCallback {

    init {
        Log.d(TAG, " license URL  : " + licUrl)
    }

    @Throws(Exception::class)
    override fun executeProvisionRequest(uuid: UUID, provisionRequest: ExoMediaDrm.ProvisionRequest): ByteArray {
        val url = provisionRequest.defaultUrl + "&signedRequest=" + String(provisionRequest.data)
        return executePost(url, null, null)
    }

    @Throws(Exception::class)
    override fun executeKeyRequest(uuid: UUID, keyRequest: ExoMediaDrm.KeyRequest): ByteArray {

        //Holder for additional req parameters, as per custom implementation
        val requestProperties = HashMap<String, String>()
        val builder = Uri.parse(licUrl).buildUpon()

        // Set content type for Widevine
        requestProperties.put("Content-Type", "text/xml")

        val uri = builder.build()
        try {
            return executePost(uri.toString(), keyRequest.data, requestProperties)
        } catch (e: FileNotFoundException) {
            throw IOException("License not found")
        } catch (e: IOException) {
            throw IOException("Error during license acquisition", e)
        }

        /*try {
            JSONObject jsonObject = new JSONObject(new String(bytes));
            return Base64.decode(jsonObject.getString("license"), Base64.DEFAULT);
        } catch (JSONException e) {
            Log.e(TAG, "Error while parsing response: " + new String(bytes), e);
            throw new RuntimeException("Error while parsing response", e);
        }*/
    }

    @Throws(IOException::class)
    private fun executePost(url: String, data: ByteArray?, requestProperties: HashMap<String, String>?): ByteArray {
        val dataSource = dataSourceFactory.createDataSource()
        if (requestProperties != null) {
            for (requestProperty in requestProperties.entries) {
                dataSource.setRequestProperty(requestProperty.key, requestProperty.value)
            }
        }
        val dataSpec = DataSpec(Uri.parse(url), data, 0, 0, C.LENGTH_UNSET.toLong(), null,
                DataSpec.FLAG_ALLOW_GZIP)
        val inputStream = DataSourceInputStream(dataSource, dataSpec)
        try {
            return Util.toByteArray(inputStream)
        } finally {
            Util.closeQuietly(inputStream)
        }
    }

    companion object {

        private val TAG = CustomDrmCallback::class.java.simpleName
        /**
         * Base URI for the License Server production environment
         */
        val LICENSE_PROD = "https://proxy.uat.widevine.com/proxy?provider=widevine_test"
        /**
         * Base URI for the License Server staging environment
         */
        val LICENSE_STAG = "https://proxy.uat.widevine.com/proxy?provider=widevine_test"
        /**
         * Base URI for the License Server test environment
         */
        val LICENSE_TEST = "https://proxy.uat.widevine.com/proxy?provider=widevine_test"
    }
}