package pibc.messagingdemo

import android.os.Bundle
import androidx.appcompat.app.AppCompatActivity
import androidx.appcompat.widget.Toolbar
import dagger.hilt.android.AndroidEntryPoint
import io.reactivex.rxjava3.android.schedulers.AndroidSchedulers
import io.reactivex.rxjava3.core.Completable
import io.reactivex.rxjava3.disposables.CompositeDisposable
import io.reactivex.rxjava3.kotlin.addTo
import javax.inject.Inject

@AndroidEntryPoint
class ConversationActivity : AppCompatActivity() {

    @Inject
    lateinit var messaging: Messaging

    @Inject
    lateinit var logger: Logger

    lateinit var toolbar: Toolbar

    private val subscriptions = CompositeDisposable()

    override fun onCreate(savedInstanceState: Bundle?) {
        super.onCreate(savedInstanceState)
        logger.v(LogTag.MSG, "$this: onCreate")
        supportActionBar!!.hide()
        setContentView(R.layout.activity_conversation)
        toolbar = findViewById(R.id.toolBar)

        toolbar.setOnMenuItemClickListener {
            when (it.itemId) {
                R.id.action_end_conversation -> messaging.finishConversation()
                else -> logger.v(LogTag.MSG, "Unrecognized menu item: $it")
            }
            true
        }

        messaging.onConversationStarted
            .observeOn(AndroidSchedulers.mainThread())
            .subscribe({
                addEndConversationButton()
            }, {
                logger.e(LogTag.MSG, "error listening for conversation started: $it")
            })
            .addTo(subscriptions)

        messaging.onConversationEnded
            .observeOn(AndroidSchedulers.mainThread())
            .subscribe({
                finish()
            }, {
                logger.e(LogTag.MSG, "error listening for conversation ended: $it")
            })
            .addTo(subscriptions)

        messaging.initialize("78100234", BuildConfig.APPLICATION_ID)
            .andThen(Completable.fromAction { messaging.registerForConversationEvents() })
            .andThen(messaging.isConversationActive)
            .observeOn(AndroidSchedulers.mainThread())
            .subscribe({ conversationIsAlreadyActive ->
                showConversationFragment()
                if (conversationIsAlreadyActive) {
                    addEndConversationButton()
                }
            }, {
                logger.e(LogTag.MSG, "messaging initialization error: $it")
            })
            .addTo(subscriptions)
    }

    override fun onDestroy() {
        super.onDestroy()
        subscriptions.dispose()
    }

    private fun showConversationFragment() {
        messaging.conversationFragment?.let {
            supportFragmentManager.beginTransaction().replace(R.id.conversation_container, it).commit()
        }
    }

    private fun addEndConversationButton() {
        toolbar.menu.add(0, R.id.action_end_conversation, 0, "End conversation")
    }
}