import { useEffect, useState } from 'react';
import { NavLink, useLocation, useNavigate } from 'react-router-dom';

import {
  Bug,
  LayoutDashboard,
  FolderKanban,
  Users,
  LogOut,
  Columns3,
  CalendarDays,
  FileText,
  ShieldCheck,
  Bell,
  Moon,
  Sun,
  Menu,
  X,
  ChevronRight,
  CircleHelp,
  Search,
  CheckCheck,
  CheckCircle2,
  CircleAlert
} from 'lucide-react';

import { useAuth } from '../context/AuthContext';
import api from '../services/api';

const navigation = [
  {
    heading: 'WORKSPACE',
    items: [
      { to: '/', label: 'Dashboard', icon: LayoutDashboard },
      { to: '/bugs', label: 'Bugs', icon: Bug },
      { to: '/kanban', label: 'Kanban board', icon: Columns3 },
      { to: '/projects', label: 'Projects', icon: FolderKanban },
      { to: '/sprints', label: 'Sprints', icon: CalendarDays },
      { to: '/reports', label: 'Reports', icon: FileText }
    ]
  }
];

export default function Layout({ children }) {
  const { user, logout } = useAuth();
  const location = useLocation();
  const navigate = useNavigate();

  const [notes, setNotes] = useState({ items: [], unread: 0 });
  const [dark, setDark] = useState(
    () => localStorage.getItem('bugtrack_theme') === 'dark'
  );
  const [mobileOpen, setMobileOpen] = useState(false);
  const [notificationsOpen, setNotificationsOpen] = useState(false);
  const [helpOpen, setHelpOpen] = useState(false);
  const [notificationError, setNotificationError] = useState('');

  const loadNotes = async () => {
    try {
      const response = await api.get('/features/notifications');
      const data = response.data || {};

      setNotes({
        items: Array.isArray(data.items) ? data.items : [],
        unread: Number(data.unread) || 0
      });

      setNotificationError('');
    } catch {
      setNotificationError('Notifications could not be refreshed.');
    }
  };

  useEffect(() => {
    document.documentElement.dataset.theme = dark ? 'dark' : 'light';
    localStorage.setItem('bugtrack_theme', dark ? 'dark' : 'light');
  }, [dark]);

  useEffect(() => {
    loadNotes();

    const timer = setInterval(loadNotes, 30000);

    return () => clearInterval(timer);
  }, []);

  useEffect(() => {
    setMobileOpen(false);
    setNotificationsOpen(false);
    setHelpOpen(false);
  }, [location.pathname]);

  useEffect(() => {
    if (!helpOpen && !notificationsOpen) return undefined;

    const handleEscape = (event) => {
      if (event.key === 'Escape') {
        setHelpOpen(false);
        setNotificationsOpen(false);
      }
    };

    window.addEventListener('keydown', handleEscape);

    return () => window.removeEventListener('keydown', handleEscape);
  }, [helpOpen, notificationsOpen]);

  const markNotificationsRead = async () => {
    setNotificationError('');

    try {
      if (notes.unread > 0) {
        await api.patch('/features/notifications/read-all');
      }

      await loadNotes();
    } catch {
      setNotificationError(
        'Unable to update notifications. Please try again.'
      );
    }
  };

  const role = String(user?.role || '')
    .replace(/^ROLE_/, '')
    .toUpperCase();

  const initials = (user?.name || user?.email || 'U')
    .trim()
    .split(/\s+/)
    .slice(0, 2)
    .map((part) => part[0])
    .join('')
    .toUpperCase();

  const visibleNavigation = navigation.map((group) => ({
    ...group,
    items: [...group.items]
  }));

  if (role === 'ADMIN') {
    visibleNavigation.push({
      heading: 'TEAM',
      items: [
        { to: '/users', label: 'Team members', icon: Users }
      ]
    });
  }

  if (role === 'ADMIN') {
    visibleNavigation.push({
      heading: 'ADMINISTRATION',
      items: [
        { to: '/security', label: 'Security center', icon: ShieldCheck }
      ]
    });
  }

  const closeMenus = () => {
    setMobileOpen(false);
    setNotificationsOpen(false);
  };

  const openBugSearch = () => {
    setNotificationsOpen(false);
    setMobileOpen(false);

    if (location.pathname !== '/bugs') {
      navigate('/bugs');
      return;
    }

    window.setTimeout(() => {
      document
        .querySelector(
          'input[type="search"], input[placeholder*="Search"], input[placeholder*="search"]'
        )
        ?.focus();
    }, 0);
  };

  const helpGuides = [
    {
      title: 'Report and manage bugs',
      description:
        'Open Bugs to report issues, search existing reports, change priorities and update statuses.'
    },
    {
      title: 'Track your workflow',
      description:
        'Use the Kanban board to follow bugs through their workflow. Open an issue to view its details.'
    },
    {
      title: 'Organize projects and sprints',
      description:
        'Use Projects to organize your work and Sprints to plan development activities.'
    },
    {
      title: 'Review reports',
      description:
        'Use Reports to review issue statistics and download available exports.'
    },
    {
      title: 'Manage your account',
      description:
        'Use the theme button to switch appearances and Sign out when you finish your session.'
    }
  ];

  return (
    <div className={`workspaceApp ${dark ? 'workspaceDark' : ''}`}>
      {mobileOpen && (
        <button
          type="button"
          className="workspaceOverlay"
          aria-label="Close navigation"
          onClick={() => setMobileOpen(false)}
        />
      )}

      <aside
        className={`workspaceSidebar ${
          mobileOpen ? 'sidebarOpen' : ''
        }`}
      >
        <div className="workspaceBrand">
          <div className="workspaceBrandIcon">
            <Bug size={23} />
          </div>

          <div className="workspaceBrandText">
            <strong>
              BugTrack<span>Pro</span>
            </strong>
            <small>Issue management</small>
          </div>

          <button
            type="button"
            className="workspaceMobileClose"
            onClick={() => setMobileOpen(false)}
            aria-label="Close menu"
          >
            <X size={20} />
          </button>
        </div>

        <div className="workspaceSwitcher">
          <div className="workspaceSwitcherIcon">E</div>

          <div className="workspaceSwitcherText">
            <strong>Engineering</strong>
            <span>Workspace</span>
          </div>

          <ChevronRight size={17} />
        </div>

        <nav className="workspaceNav" aria-label="Main navigation">
          {visibleNavigation.map((group) => (
            <div className="workspaceNavGroup" key={group.heading}>
              <div className="workspaceNavHeading">
                {group.heading}
              </div>

              {group.items.map((item) => {
                const Icon = item.icon;

                return (
                  <NavLink
                    key={item.to}
                    to={item.to}
                    end={item.to === '/'}
                    className={({ isActive }) =>
                      `workspaceNavLink ${
                        isActive ? 'workspaceNavActive' : ''
                      }`
                    }
                    onClick={closeMenus}
                  >
                    <Icon size={19} strokeWidth={1.9} />
                    <span>{item.label}</span>
                  </NavLink>
                );
              })}
            </div>
          ))}
        </nav>

        <div className="workspaceSidebarBottom">
          <button
            type="button"
            className="workspaceHelp"
            onClick={() => setHelpOpen(true)}
            aria-label="Open BugTrackPro help"
          >
            <div className="workspaceHelpIcon">
              <CircleHelp size={19} />
            </div>

            <div className="workspaceHelpText">
              <strong>Need help?</strong>
              <span>Manage issues with ease.</span>
            </div>
          </button>

          <button
            type="button"
            className="workspaceSignOut"
            onClick={logout}
          >
            <LogOut size={18} />
            <span>Sign out</span>
          </button>

          <div className="workspaceAccount">
            <div className="workspaceAvatar">{initials}</div>

            <div className="workspaceAccountText">
              <strong>{user?.name || 'Workspace user'}</strong>
              <span>{role || 'Member'}</span>
            </div>

            <ShieldCheck
              size={17}
              className="workspaceAccountShield"
            />
          </div>
        </div>
      </aside>

      <div className="workspaceMain">
        <header className="workspaceTopbar">
          <div className="workspaceTopbarLeft">
            <button
              type="button"
              className="workspaceIconButton workspaceMenuButton"
              onClick={() => setMobileOpen(true)}
              aria-label="Open navigation"
            >
              <Menu size={21} />
            </button>

            <div className="workspaceBreadcrumb">
              <span>Workspace</span>
              <ChevronRight size={15} />

              <strong>
                {location.pathname === '/'
                  ? 'Dashboard'
                  : visibleNavigation
                      .flatMap((group) => group.items)
                      .find((item) => item.to === location.pathname)
                      ?.label ||
                    (location.pathname.startsWith('/bugs/')
                      ? 'Bug details'
                      : location.pathname.startsWith('/projects/')
                        ? 'Project details'
                        : 'Workspace')}
              </strong>
            </div>
          </div>

          <div className="workspaceTopbarActions">
            <button
              type="button"
              className="workspaceSearchButton"
              onClick={openBugSearch}
              title="Find bugs"
            >
              <Search size={17} />
              <span>Find bugs</span>
              <kbd>/</kbd>
            </button>

            <div className="workspaceNotificationWrap">
              <button
                type="button"
                className={`workspaceIconButton ${
                  notificationsOpen ? 'workspaceToolActive' : ''
                }`}
                onClick={() =>
                  setNotificationsOpen((value) => !value)
                }
                aria-label={`Notifications${
                  notes.unread ? `, ${notes.unread} unread` : ''
                }`}
                aria-expanded={notificationsOpen}
                title="Notifications"
              >
                <Bell size={19} />

                {notes.unread > 0 && (
                  <span className="workspaceNotificationDot">
                    {notes.unread > 99 ? '99+' : notes.unread}
                  </span>
                )}
              </button>

              {notificationsOpen && (
                <>
                  <button
                    type="button"
                    className="workspacePopupDismiss"
                    aria-label="Close notifications"
                    onClick={() => setNotificationsOpen(false)}
                  />

                  <section
                    className="workspaceNotificationPanel"
                    aria-label="Notifications"
                  >
                    <div className="workspaceNotificationHeader">
                      <div>
                        <strong>Notifications</strong>
                        <span>
                          {notes.unread
                            ? `${notes.unread} unread`
                            : 'You are all caught up'}
                        </span>
                      </div>

                      <button
                        type="button"
                        className="workspaceMarkRead"
                        onClick={markNotificationsRead}
                        title="Mark all as read"
                        aria-label="Mark all notifications as read"
                      >
                        <CheckCheck size={18} />
                      </button>
                    </div>

                    {notificationError && (
                      <p className="workspaceNotificationError" role="alert">
                        {notificationError}
                      </p>
                    )}

                    <div className="workspaceNotificationList">
                      {notes.items.length === 0 ? (
                        <div className="workspaceNotificationEmpty">
                          <Bell size={24} />
                          <strong>No notifications yet</strong>
                          <span>Updates will appear here.</span>
                        </div>
                      ) : (
                        notes.items.slice(0, 8).map((note) => (
                          <div
                            className={`workspaceNotificationItem ${
                              note.read ? '' : 'notificationUnread'
                            }`}
                            key={note.id}
                          >
                            <span className="workspaceNotificationIcon">
                              <Bell size={16} />
                            </span>

                            <div>
                              <strong>{note.title}</strong>
                              <p>{note.message}</p>
                            </div>

                            {!note.read && (
                              <span
                                className="workspaceUnreadIndicator"
                                title="Unread"
                              />
                            )}
                          </div>
                        ))
                      )}
                    </div>

                    <button
                      type="button"
                      className="workspaceNotificationFooter"
                      onClick={markNotificationsRead}
                    >
                      <CheckCircle2 size={16} />
                      Mark all as read
                    </button>
                  </section>
                </>
              )}
            </div>

            <button
              type="button"
              className="workspaceIconButton"
              onClick={() => setDark((value) => !value)}
              aria-label={
                dark ? 'Switch to light theme' : 'Switch to dark theme'
              }
              title={dark ? 'Light mode' : 'Dark mode'}
            >
              {dark ? <Sun size={19} /> : <Moon size={19} />}
            </button>

            <div className="workspaceTopbarDivider" />

            <div className="workspaceTopProfile">
              <div className="workspaceAvatar">{initials}</div>

              <div className="workspaceTopProfileText">
                <strong>{user?.name || 'Workspace user'}</strong>
                <span>{role || 'Member'}</span>
              </div>
            </div>
          </div>
        </header>

        <main className="workspaceContent">
          {children}
        </main>
      </div>

      {helpOpen && (
        <div
          className="workspaceHelpOverlay"
          onClick={() => setHelpOpen(false)}
        >
          <section
            className="workspaceHelpDialog"
            role="dialog"
            aria-modal="true"
            aria-labelledby="workspaceHelpTitle"
            onClick={(event) => event.stopPropagation()}
          >
            <header className="workspaceHelpDialogHeader">
              <div className="workspaceHelpIcon">
                <CircleHelp size={22} />
              </div>

              <div>
                <h2 id="workspaceHelpTitle">BugTrackPro Help</h2>
                <p>Quick guide to your workspace</p>
              </div>

              <button
                type="button"
                className="workspaceIconButton"
                aria-label="Close help"
                onClick={() => setHelpOpen(false)}
              >
                <X size={20} />
              </button>
            </header>

            <p className="workspaceHelpIntro">
              Find your way around BugTrackPro with these quick tips.
            </p>

            <div className="workspaceHelpGuides">
              {helpGuides.map((guide, index) => (
                <div className="workspaceHelpGuide" key={guide.title}>
                  <div className="workspaceHelpGuideNumber">
                    {index + 1}
                  </div>

                  <div>
                    <strong>{guide.title}</strong>
                    <p>{guide.description}</p>
                  </div>
                </div>
              ))}
            </div>

            <div className="workspaceHelpNotice">
              <CircleAlert size={17} />
              <span>
                Available actions depend on your assigned account role.
              </span>
            </div>

            <button
              type="button"
              className="workspaceHelpDone"
              onClick={() => setHelpOpen(false)}
            >
              Got it
            </button>
          </section>
        </div>
      )}
    </div>
  );
}
