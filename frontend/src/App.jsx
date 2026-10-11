import { Navigate, Route, Routes } from 'react-router-dom';
import { useAuth } from './context/AuthContext';

import Layout from './components/Layout';

import Login from './pages/Login';
import Register from './pages/Register';
import Dashboard from './pages/Dashboard';
import Bugs from './pages/Bugs';
import BugDetails from './pages/BugDetails';
import Projects from './pages/Projects';
import Users from './pages/Users';
import Kanban from './pages/Kanban';
import Sprints from './pages/Sprints';
import Reports from './pages/Reports';
import Security from './pages/Security';

function Private({ children }) {
    const { user } = useAuth();

    return user ? (
        <Layout>{children}</Layout>
    ) : (
        <Navigate to="/login" replace />
    );
}

function Role({ roles, children }) {
    const { user } = useAuth();

    return roles.includes(user?.role) ? (
        children
    ) : (
        <Navigate to="/" replace />
    );
}

export default function App() {
    return (
        <Routes>
            <Route
                path="/login"
                element={<Login />}
            />

            <Route
                path="/register"
                element={<Register />}
            />

            <Route
                path="/"
                element={
                    <Private>
                        <Dashboard />
                    </Private>
                }
            />

            <Route
                path="/bugs"
                element={
                    <Private>
                        <Bugs />
                    </Private>
                }
            />

            <Route
                path="/bugs/:id"
                element={
                    <Private>
                        <BugDetails />
                    </Private>
                }
            />

            <Route
                path="/projects"
                element={
                    <Private>
                        <Projects />
                    </Private>
                }
            />

            <Route
                path="/kanban"
                element={
                    <Private>
                        <Kanban />
                    </Private>
                }
            />

            <Route
                path="/sprints"
                element={
                    <Private>
                        <Sprints />
                    </Private>
                }
            />

            <Route
                path="/reports"
                element={
                    <Private>
                        <Reports />
                    </Private>
                }
            />

            <Route
                path="/users"
                element={
                    <Private>
                        <Role roles={['ADMIN']}>
                            <Users />
                        </Role>
                    </Private>
                }
            />

            <Route
                path="/security"
                element={
                    <Private>
                        <Role roles={['ADMIN']}>
                            <Security />
                        </Role>
                    </Private>
                }
            />

            <Route
                path="*"
                element={<Navigate to="/" replace />}
            />
        </Routes>
    );
}
