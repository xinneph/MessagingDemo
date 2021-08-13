package pibc.messagingdemo

import android.content.Context
import androidx.fragment.app.Fragment
import com.liveperson.api.LivePersonCallback
import com.liveperson.api.response.types.CloseReason
import com.liveperson.api.sdk.LPConversationData
import com.liveperson.api.sdk.PermissionType
import com.liveperson.infra.ConversationViewParams
import com.liveperson.infra.ICallback
import com.liveperson.infra.InitLivePersonProperties
import com.liveperson.infra.MonitoringInitParams
import com.liveperson.infra.auth.LPAuthenticationParams
import com.liveperson.infra.auth.LPAuthenticationType
import com.liveperson.infra.callbacks.InitLivePersonCallBack
import com.liveperson.infra.log.LogLevel
import com.liveperson.messaging.LpError
import com.liveperson.messaging.TaskType
import com.liveperson.messaging.model.AgentData
import com.liveperson.messaging.sdk.api.LivePerson
import com.liveperson.messaging.sdk.api.model.ConsumerProfile
import io.reactivex.rxjava3.core.Completable
import io.reactivex.rxjava3.core.Observable
import io.reactivex.rxjava3.core.Single
import io.reactivex.rxjava3.subjects.PublishSubject
import javax.inject.Inject
import javax.inject.Singleton

@Singleton
class Messaging @Inject constructor(
    private val applicationContext: Context,
    private val logger: Logger,
){
    private val _onConversationStarted = PublishSubject.create<String>()
    private val _onConversationEnded = PublishSubject.create<String>()

    val conversationFragment: Fragment? get() {
        val authParams = LPAuthenticationParams(LPAuthenticationType.UN_AUTH)
        return LivePerson.getConversationFragment(authParams, ConversationViewParams(false))
    }

    val onConversationStarted: Observable<String> = _onConversationStarted
    val onConversationEnded: Observable<String> = _onConversationEnded

    fun initialize(brandId: String, applicationId: String): Completable = Completable.create { emitter ->
        LivePerson.Logging.setSDKLoggingLevel(if (BuildConfig.DEBUG) LogLevel.INFO else LogLevel.ERROR)
        logger.i(LogTag.MSG, "LP initialization started.")
        val installationId = "f10767f2-24fd-4d8f-95bd-0397cd244194"
        val monitoringInitParams = MonitoringInitParams(installationId)
        logger.d(LogTag.MSG, "LP installation Id: ${monitoringInitParams.appInstallId}")
        LivePerson.initialize(
            applicationContext,
            InitLivePersonProperties(brandId, applicationId, monitoringInitParams, object : InitLivePersonCallBack {
                override fun onInitFailed(exception: Exception) {
                    val msg = "SDK initialization failed for account $brandId."
                    logger.e(LogTag.MSG, msg)
                    emitter.onError(exception)
                }

                override fun onInitSucceed() {
                    logger.i(LogTag.MSG, "LP initialization succeeded for brand $brandId")
                    LivePerson.setUserProfile(
                        ConsumerProfile.Builder()
                            .setFirstName("")
                            .setLastName("")
                            .setPhoneNumber("")
                            .build()
                    )
                    emitter.onComplete()
                }
            })
        )
    }

    fun registerForConversationEvents() {
        logger.v(LogTag.MSG, "Registering for conversation events")
        LivePerson.setCallback(object : LivePersonCallback {

            override fun onConversationStarted(convData: LPConversationData) {
                logger.v(LogTag.MSG, "onConversationStarted(${convData.id})")
                _onConversationStarted.onNext(convData.id)
            }

            override fun onConversationStarted() {}

            override fun onConversationResolved(convData: LPConversationData) {
                logger.v(LogTag.MSG, "onConversationResolved($convData)")
                _onConversationEnded.onNext(convData.id)
            }

            override fun onConversationResolved() {
                logger.v(LogTag.MSG, "onConversationResolved()")
                _onConversationEnded.onNext("")
            }

            override fun onConversationResolved(reason: CloseReason) {
                logger.v(LogTag.MSG, "onConversationResolved($reason)")
                _onConversationEnded.onNext(reason.name)
            }

            override fun onConversationFragmentClosed() {}

            override fun onConversationMarkedAsUrgent() {}

            override fun onConversationMarkedAsNormal() {}

            override fun onAgentTyping(isTyping: Boolean) {}

            override fun onAgentDetailsChanged(agentData: AgentData?) {}

            override fun onOfflineHoursChanges(isOfflineHoursOn: Boolean) {}

            override fun onUserDeniedPermission(permissionType: PermissionType?, doNotShowAgainMarked: Boolean) {}

            override fun onUserActionOnPreventedPermission(permissionType: PermissionType?) {}

            override fun onAgentAvatarTapped(agentData: AgentData?) {}

            override fun onStructuredContentLinkClicked(uri: String?) {}

            override fun onCsatLaunched() {}

            override fun onCsatDismissed() {}

            override fun onCsatSubmitted(conversationId: String?) {}

            override fun onCsatSubmitted(conversationId: String?, starRating: Int) {}

            override fun onCsatSkipped() {}

            override fun onTokenExpired() {}

            override fun onUnauthenticatedUserExpired() {}

            override fun onConnectionChanged(isConnected: Boolean) {}

            override fun onError(type: TaskType?, message: String?) {}

            override fun onError(lpError: LpError?, message: String?) {}
        })
    }

    fun finishConversation() {
        LivePerson.resolveConversation()
    }

    val isConversationActive: Single<Boolean> = Single.create { emitter ->
        logger.d(LogTag.MSG, "checking active conversation state...")
        LivePerson.checkActiveConversation(object : ICallback<Boolean, Exception> {
            override fun onSuccess(isActive: Boolean) {
                logger.d(LogTag.MSG, "isConversationActive: $isActive")
                emitter.onSuccess(isActive)
            }

            override fun onError(error: Exception) {
                emitter.onError(error)
            }
        })
    }
}