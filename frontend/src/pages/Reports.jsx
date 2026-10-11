import { useEffect, useState } from 'react';
import { BarChart3, Download, Printer, FileText, RefreshCw } from 'lucide-react';
import api from '../services/api';

const statusLabels = {
  OPEN: 'Open',
  IN_PROGRESS: 'In progress',
  RESOLVED: 'Resolved',
  CLOSED: 'Closed',
  REOPENED: 'Reopened'
};

export default function Reports() {
  const [analytics, setAnalytics] = useState(null);
  const [loading, setLoading] = useState(true);
  const [exporting, setExporting] = useState(false);
  const [error, setError] = useState('');

  const load = async () => {
    setLoading(true);
    try {
      const response = await api.get('/features/analytics');
      setAnalytics(response.data);
      setError('');
    } catch (e) {
      setError(e.response?.data?.error || 'Reports could not be loaded. Check your access or connection and retry.');
    } finally {
      setLoading(false);
    }
  };

  useEffect(() => { load(); }, []);

  const downloadCsv = async () => {
    setExporting(true);
    setError('');
    try {
      const response = await api.get('/features/reports/bugs.csv', { responseType: 'blob' });
      const url = URL.createObjectURL(response.data);
      const link = document.createElement('a');
      link.href = url;
      link.download = 'bug-report.csv';
      document.body.appendChild(link);
      link.click();
      link.remove();
      window.setTimeout(() => URL.revokeObjectURL(url), 1000);
    } catch (e) {
      setError(e.response?.data?.error || 'The CSV report could not be downloaded. Please try again.');
    } finally {
      setExporting(false);
    }
  };

  if (loading && !analytics) return <div className="loading">Loading reports…</div>;
  if (!analytics) return <>
    <div className="pageHead"><div><div className="eyebrow">INSIGHTS & EXPORTS</div><h1>Reports</h1><p>Operational analytics for managers, testers and engineering leads.</p></div></div>
    <div className="inlineError" role="alert">{error || 'Reports are unavailable.'}</div>
    <button className="secondary" onClick={load} disabled={loading}><RefreshCw size={16}/> Retry</button>
  </>;

  const byStatus = analytics.byStatus || {};
  const statusEntries = Object.entries(byStatus).sort(([a], [b]) => a.localeCompare(b));
  const issueCount = statusEntries.reduce((total, [, count]) => total + Number(count || 0), 0);
  const maxStatusCount = Math.max(1, ...statusEntries.map(([, count]) => Number(count || 0)));
  const generatedAt = new Date().toLocaleString();

  return <div className="reportsPage">
    <div className="reportsScreen">
      <div className="pageHead">
        <div><div className="eyebrow">INSIGHTS & EXPORTS</div><h1>Reports</h1><p>Operational analytics for managers, testers and engineering leads.</p></div>
        <div className="headActions">
          <button className="secondary" onClick={load} disabled={loading}><RefreshCw size={16}/> {loading ? 'Refreshing…' : 'Refresh'}</button>
          <button className="secondary" onClick={downloadCsv} disabled={exporting}><Download size={16}/> {exporting ? 'Preparing…' : 'CSV'}</button>
          <button className="primary" onClick={() => window.print()}><Printer size={16}/> Print / PDF</button>
        </div>
      </div>

      {error && <div className="inlineError" role="alert">{error}</div>}

      <div className="grid3">
        <section className="panel"><BarChart3/><h2>Average resolution</h2><div className="metricBig">{analytics.avgResolutionHours ?? 0}h</div><span className="muted">Based on issues with recorded resolution time.</span></section>
        <section className="panel"><FileText/><h2>SLA breaches</h2><div className="metricBig dangerText">{analytics.overdue ?? 0}</div><span className="muted">Open issues past their SLA deadline.</span></section>
        <section className="panel"><Download/><h2>Export center</h2><p>Download the issue dataset available to your account for spreadsheet or audit workflows.</p><button className="secondary" onClick={downloadCsv} disabled={exporting}><Download size={16}/> {exporting ? 'Preparing…' : 'Download CSV'}</button></section>
      </div>

      <section className="panel reportChart">
        <div className="reportChartHeading"><div><h2>Status distribution</h2><p>{issueCount} {issueCount === 1 ? 'issue' : 'issues'} in the current report</p></div></div>
        {statusEntries.length > 0 ? <div className="reportRows">{statusEntries.map(([status, count]) => <div className="reportRow" key={status}>
          <span>{statusLabels[status] || status.replaceAll('_', ' ')}</span>
          <div className="bar"><i style={{ width: `${Number(count || 0) / maxStatusCount * 100}%` }}/></div>
          <b>{count}</b>
        </div>)}</div> : <div className="empty reportEmpty">No issues are available in your current project access yet.</div>}
      </section>
    </div>

    <section className="panel printOnly" aria-label="Printable management report">
      <h2>BugTrackPro — Management Report</h2>
      <p>Generated {generatedAt}</p>
      <div className="printSummary">
        <div><span>Average resolution</span><b>{analytics.avgResolutionHours ?? 0} hours</b></div>
        <div><span>SLA breaches</span><b>{analytics.overdue ?? 0}</b></div>
        <div><span>Issues in report</span><b>{issueCount}</b></div>
      </div>
      <h3>Status distribution</h3>
      {statusEntries.length > 0 ? <table className="printTable"><thead><tr><th>Status</th><th>Issues</th></tr></thead><tbody>{statusEntries.map(([status, count]) => <tr key={status}><td>{statusLabels[status] || status.replaceAll('_', ' ')}</td><td>{count}</td></tr>)}</tbody></table> : <p>No issues are available in your current project access.</p>}
    </section>
  </div>;
}
