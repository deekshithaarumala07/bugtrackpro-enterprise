import { useEffect, useState } from 'react';
import { Clock3, RefreshCw } from 'lucide-react';
import { Link } from 'react-router-dom';
import api from '../services/api';
import { useAuth } from '../context/AuthContext';

const columns = [
  ['OPEN', 'Open'],
  ['IN_PROGRESS', 'In progress'],
  ['RESOLVED', 'Resolved'],
  ['CLOSED', 'Closed'],
  ['REOPENED', 'Reopened'],
];

const transitions = {
  OPEN: ['IN_PROGRESS', 'REOPENED'],
  IN_PROGRESS: ['OPEN', 'RESOLVED'],
  RESOLVED: ['IN_PROGRESS', 'CLOSED', 'REOPENED'],
  CLOSED: ['REOPENED'],
  REOPENED: ['IN_PROGRESS', 'RESOLVED'],
};

function allowedTargets(bug, user) {
  const validTargets = transitions[bug.status] || [];
  if (user?.role === 'ADMIN'
      || (user?.role === 'MANAGER' && String(bug.project?.manager?.id) === String(user.id))) {
    return validTargets;
  }
  if (user?.role === 'DEVELOPER' && String(bug.assignee?.id) === String(user.id)) {
    return validTargets.filter(status => ['IN_PROGRESS', 'RESOLVED'].includes(status));
  }
  if (user?.role === 'TESTER' && String(bug.reporter?.id) === String(user.id)) {
    return validTargets.filter(status => status === 'REOPENED');
  }
  return [];
}

export default function Kanban() {
  const { user } = useAuth();
  const [bugs, setBugs] = useState([]);
  const [error, setError] = useState('');
  const [loading, setLoading] = useState(true);
  const [busyId, setBusyId] = useState(null);

  const load = async () => {
    setLoading(true);
    try {
      const response = await api.get('/bugs');
      setBugs(response.data);
      setError('');
    } catch (e) {
      setError(e.response?.data?.error || 'The board could not load. Check your access or connection and retry.');
    } finally {
      setLoading(false);
    }
  };

  useEffect(() => { load(); }, []);

  const move = async (id, status) => {
    if (busyId !== null) return;
    const bug = bugs.find(item => String(item.id) === String(id));
    if (!bug || bug.status === status || !allowedTargets(bug, user).includes(status)) return;

    setBusyId(String(id));
    setError('');
    try {
      await api.patch(`/bugs/${id}/status`, { status });
      await load();
    } catch (e) {
      setError(e.response?.data?.error || 'The status change was rejected. The issue stayed in its previous column.');
    } finally {
      setBusyId(null);
    }
  };

  return <>
    <div className="pageHead">
      <div>
        <div className="eyebrow">WORKFLOW</div>
        <h1>Kanban board</h1>
        <p>Move issues through the workflow. The server checks each transition and your permissions.</p>
      </div>
      <button className="secondary" onClick={load} disabled={loading || busyId !== null}>
        <RefreshCw size={16} aria-hidden="true"/> Refresh
      </button>
    </div>

    {error && <div className="inlineError" role="alert">{error}</div>}

    <div className="kanban" aria-label="Issue workflow board" aria-busy={loading || busyId !== null}>
      {columns.map(([status, label]) => {
        const issues = bugs.filter(bug => bug.status === status);
        return <section
          className="kanbanCol"
          key={status}
          data-status={status}
          aria-labelledby={`kanban-heading-${status}`}
          onDragOver={event => { if (busyId === null) event.preventDefault(); }}
          onDrop={event => {
            event.preventDefault();
            move(event.dataTransfer.getData('text/plain'), status);
          }}
        >
          <div className="kanbanHead">
            <h2 id={`kanban-heading-${status}`}>{label}</h2>
            <span className="kanbanCount" aria-label={`${issues.length} issues`}>{issues.length}</span>
          </div>

          <div className="kanbanCards">
            {issues.map(bug => {
              const targets = allowedTargets(bug, user);
              return <article
                className={`kanbanCard ${busyId === String(bug.id) ? 'kanbanCardBusy' : ''}`}
                key={bug.id}
                draggable={targets.length > 0 && busyId === null}
                onDragStart={event => {
                  event.dataTransfer.effectAllowed = 'move';
                  event.dataTransfer.setData('text/plain', String(bug.id));
                }}
              >
                <div className="kanbanCardTop">
                  <span className="kanbanIssueId">#{bug.id}</span>
                  <span className={`priority p-${String(bug.priority || 'medium').toLowerCase()}`}>{bug.priority}</span>
                </div>
                <Link className="kanbanIssueTitle" to={`/bugs/${bug.id}`}>{bug.title}</Link>
                <div className="kanbanCardProject">{bug.project?.name || 'Project'}</div>
                <div className="kanbanCardMeta">
                  <span className="kanbanAssignee">{bug.assignee?.name || 'Unassigned'}</span>
                  {bug.dueDate && <span className="kanbanDue"><Clock3 size={13} aria-hidden="true"/>{new Date(bug.dueDate).toLocaleDateString()}</span>}
                </div>
                {targets.length > 0 && <label className="kanbanMoveControl">
                  <span>Move to</span>
                  <select
                    value={bug.status}
                    disabled={busyId !== null}
                    aria-label={`Change status for issue ${bug.id}`}
                    onChange={event => move(bug.id, event.target.value)}
                  >
                    <option value={bug.status}>{columns.find(([value]) => value === bug.status)?.[1] || bug.status}</option>
                    {targets.map(target => <option value={target} key={target}>
                      {columns.find(([value]) => value === target)?.[1] || target}
                    </option>)}
                  </select>
                </label>}
              </article>;
            })}
            {issues.length === 0 && <div className="kanbanEmpty">
              {loading ? 'Loading issues…' : 'No issues in this column'}
            </div>}
          </div>
        </section>;
      })}
    </div>
  </>;
}
