package com.solarrobo.core.contracts

/** Local, offline access state. VIEWER is read-only; OPERATOR may issue authorized tracker commands. */
enum class AccessMode { VIEWER, OPERATOR }
