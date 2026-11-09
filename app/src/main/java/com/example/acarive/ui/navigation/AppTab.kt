package com.example.acarive.ui.navigation

import com.example.acarive.ui.components.AppIcon

enum class AppTab(val label: String, val icon: AppIcon) {
    Home("홈", AppIcon.Home),
    Route("코스", AppIcon.Route),
    Record("기록", AppIcon.History),
    Saved("저장", AppIcon.Heart),
    My("마이", AppIcon.User),
}
