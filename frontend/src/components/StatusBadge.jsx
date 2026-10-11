export default function StatusBadge({value}){return <span className={`badge status-${value?.toLowerCase().replace('_','-')}`}>{value?.replace('_',' ')}</span>}
