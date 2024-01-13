package cc.unitmesh.devti.actions.vcs

import cc.unitmesh.devti.statusbar.AutoDevStatus

interface OnEventListener {
    fun onEventOccurred(status: AutoDevStatus)
}

class StatusEventProducer {
    private var listener: OnEventListener? = null

    fun setOnEventListener(listener: OnEventListener) {
        this.listener = listener
    }

    fun triggerEvent(status: AutoDevStatus) {
        listener?.onEventOccurred(status)
    }
}