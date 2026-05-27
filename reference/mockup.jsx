import React, { useState } from 'react';
import { 
  LayoutDashboard, 
  Calendar, 
  MessageSquare, 
  Users, 
  Receipt, 
  Settings,
  Menu,
  Plus,
  ArrowLeft,
  MapPin,
  Clock,
  Car,
  CheckCircle2,
  Play,
  Navigation,
  Phone,
  MoreVertical,
  DollarSign,
  ChevronLeft,
  ChevronRight,
  Filter,
  Wrench
} from 'lucide-react';

// --- Domain & Mock Data ---

// Fiduciary Rule: All money handled as cents
const formatCents = (cents) => `$${(cents / 100).toFixed(2)}`;

const MOCK_CUSTOMERS = [
  { id: 'c1', firstName: 'John', lastName: 'Doe', phone: '(555) 123-4567', email: 'john@example.com' },
  { id: 'c2', firstName: 'Sarah', lastName: 'Connor', phone: '(555) 987-6543', email: 'sarah@example.com' }
];

const MOCK_VEHICLES = [
  { id: 'v1', customerId: 'c1', year: '2018', make: 'Ford', model: 'F-150', engine: '5.0L V8' },
  { id: 'v2', customerId: 'c2', year: '2015', make: 'Honda', model: 'Civic', engine: '1.8L I4' }
];

const MOCK_JOBS = [
  { 
    id: 'j1', customerId: 'c1', vehicleId: 'v1', 
    description: 'Brake Pad Replacement (Front & Rear)', 
    status: 'ON_SITE', 
    scheduledStart: 'Today, 10:00 AM',
    address: '123 Main St, Greenville, SC',
    invoicedCents: 35000
  },
  { 
    id: 'j2', customerId: 'c2', vehicleId: 'v2', 
    description: 'Alternator Diagnostic & Replace', 
    status: 'SCHEDULED', 
    scheduledStart: 'Today, 2:00 PM',
    address: '456 Oak Ave, Spartanburg, SC',
    invoicedCents: 0
  },
  { 
    id: 'j3', customerId: 'c1', vehicleId: 'v1', 
    description: 'Routine Oil Change', 
    status: 'COMPLETE', 
    scheduledStart: 'Yesterday, 9:00 AM',
    address: '123 Main St, Greenville, SC',
    invoicedCents: 8500
  }
];

// Schedule Data (Customer Jobs + Internal Business Blocks)
const MOCK_SCHEDULE_EVENTS = [
  { id: 'e1', type: 'JOB', jobId: 'j1', title: 'Brake Pad Replacement (Front & Rear)', time: '10:00 AM', date: 26, duration: 2, color: 'blue' },
  { id: 'e2', type: 'ADMIN', title: 'Silverado Maintenance (Oil & Fluids)', time: '12:30 PM', date: 26, duration: 1, color: 'slate' },
  { id: 'e3', type: 'JOB', jobId: 'j2', title: 'Alternator Diagnostic & Replace', time: '2:00 PM', date: 26, duration: 2, color: 'blue' },
  { id: 'e4', type: 'ADMIN', title: 'Supply Run (AutoZone)', time: '4:30 PM', date: 26, duration: 1, color: 'slate' },
  { id: 'e5', type: 'ADMIN', title: 'Accounting & Invoicing', time: '9:00 AM', date: 28, duration: 3, color: 'slate' },
  { id: 'e6', type: 'JOB', title: 'Transmission Fluid Flush', time: '1:00 PM', date: 28, duration: 2, color: 'blue' }
];

export default function PhillApp() {
  const [currentTab, setCurrentTab] = useState('dashboard');
  const [navStack, setNavStack] = useState([]);
  const [scheduleView, setScheduleView] = useState('day'); // 'day', 'week', 'month'

  const pushView = (view, data = null) => setNavStack([...navStack, { view, data }]);
  const popView = () => setNavStack(navStack.slice(0, -1));
  const currentView = navStack.length > 0 ? navStack[navStack.length - 1] : { view: currentTab };

  // --- Sub-components for Screens ---

  const renderDashboard = () => (
    <div className="flex-1 overflow-y-auto bg-gray-50 p-4 pb-24 space-y-4">
      <div className="flex justify-between items-center mb-6">
        <div>
          <h1 className="text-2xl font-bold text-slate-900">Dashboard</h1>
          <p className="text-sm text-slate-500">Phillips Mobile Auto • May 26, 2026</p>
        </div>
        <div className="w-10 h-10 bg-slate-900 text-white rounded-full flex items-center justify-center font-bold">P</div>
      </div>

      <div className="grid grid-cols-2 gap-4">
        <div className="bg-white p-4 rounded-2xl shadow-sm border border-slate-100 flex flex-col justify-between">
          <div className="flex justify-between items-start mb-2">
            <Calendar className="text-blue-600" size={20} />
            <span className="text-xs font-bold bg-blue-50 text-blue-700 px-2 py-1 rounded-md">2 Active</span>
          </div>
          <div>
            <p className="text-sm text-slate-500 font-medium">Appointments</p>
            <p className="text-2xl font-bold text-slate-900">4</p>
          </div>
        </div>

        <div className="bg-white p-4 rounded-2xl shadow-sm border border-slate-100 flex flex-col justify-between">
          <div className="flex justify-between items-start mb-2">
            <DollarSign className="text-emerald-600" size={20} />
          </div>
          <div>
            <p className="text-sm text-slate-500 font-medium">Revenue (Est)</p>
            <p className="text-2xl font-bold text-slate-900">{formatCents(43500)}</p>
          </div>
        </div>
      </div>

      {/* Unified Schedule View on Dashboard */}
      <div className="bg-white rounded-2xl shadow-sm border border-slate-100 p-4 mt-6">
        <div className="flex justify-between items-end mb-4">
          <h2 className="text-sm font-bold text-slate-800 uppercase tracking-wider">Today's Schedule</h2>
          <button onClick={() => setCurrentTab('schedule')} className="text-xs font-bold text-blue-600 uppercase">View All</button>
        </div>
        
        {MOCK_SCHEDULE_EVENTS.filter(e => e.date === 26).map(evt => {
          const isJob = evt.type === 'JOB';
          const jobData = isJob ? MOCK_JOBS.find(j => j.id === evt.jobId) : null;
          
          return (
            <div 
              key={evt.id} 
              className={`mb-4 last:mb-0 p-3 rounded-xl border cursor-pointer active:scale-[0.98] transition-transform ${
                isJob ? 'bg-slate-50 border-slate-100' : 'bg-slate-800 border-slate-700 text-white'
              }`}
              onClick={() => {
                if (isJob && jobData) {
                  const customer = MOCK_CUSTOMERS.find(c => c.id === jobData.customerId);
                  const vehicle = MOCK_VEHICLES.find(v => v.id === jobData.vehicleId);
                  pushView('job_detail', { job: jobData, customer, vehicle });
                }
              }}
            >
              <div className="flex justify-between items-start">
                <p className={`font-semibold ${isJob ? 'text-slate-900' : 'text-white'}`}>{evt.time}</p>
                {isJob && jobData && (
                  <span className={`text-[10px] font-bold px-2 py-1 rounded uppercase ${
                    jobData.status === 'ON_SITE' ? 'bg-amber-100 text-amber-800' : 'bg-blue-100 text-blue-800'
                  }`}>
                    {jobData.status.replace('_', ' ')}
                  </span>
                )}
                {!isJob && (
                   <span className="text-[10px] font-bold px-2 py-1 rounded uppercase bg-slate-700 text-slate-300">
                     Internal
                   </span>
                )}
              </div>
              <p className={`text-sm font-medium mt-1 ${isJob ? 'text-slate-800' : 'text-slate-200'}`}>
                {evt.title}
              </p>
              {isJob && jobData && (
                <div className="flex items-center text-xs text-slate-500 mt-2">
                  <MapPin size={12} className="mr-1" />
                  <span className="truncate">{jobData.address}</span>
                </div>
              )}
            </div>
          );
        })}
      </div>

      {/* Unpaid Invoices */}
      <div className="bg-white rounded-2xl shadow-sm border border-slate-100 p-4 mt-6 mb-4">
        <div className="flex justify-between items-end mb-4">
          <h2 className="text-sm font-bold text-slate-800 uppercase tracking-wider">Unpaid Invoices</h2>
          <div className="text-right">
            <span className="text-[10px] font-bold text-slate-400 uppercase tracking-wider block">Total Outstanding</span>
            <span className="text-lg font-bold text-red-600">{formatCents(120000)}</span>
          </div>
        </div>
        
        <div className="space-y-3">
          <div className="flex justify-between items-center p-3 bg-red-50 rounded-xl border border-red-100 active:scale-[0.98] transition-transform cursor-pointer">
            <div className="flex items-center">
              <div className="w-8 h-8 rounded-full bg-red-100 text-red-600 flex items-center justify-center mr-3">
                <Receipt size={16} />
              </div>
              <div>
                <p className="text-sm font-bold text-slate-900">Sarah Connor</p>
                <p className="text-[10px] text-red-600 font-semibold uppercase tracking-wide mt-0.5">Overdue • Inv #1042</p>
              </div>
            </div>
            <span className="font-bold text-slate-900">{formatCents(85000)}</span>
          </div>
          
          <div className="flex justify-between items-center p-3 bg-slate-50 rounded-xl border border-slate-100 active:scale-[0.98] transition-transform cursor-pointer">
            <div className="flex items-center">
              <div className="w-8 h-8 rounded-full bg-slate-200 text-slate-500 flex items-center justify-center mr-3">
                <Receipt size={16} />
              </div>
              <div>
                <p className="text-sm font-bold text-slate-900">John Doe</p>
                <p className="text-[10px] text-slate-500 font-semibold uppercase tracking-wide mt-0.5">Sent Today • Inv #1045</p>
              </div>
            </div>
            <span className="font-bold text-slate-900">{formatCents(35000)}</span>
          </div>
        </div>
      </div>

      {/* Daily Route Map */}
      <div className="bg-white rounded-2xl shadow-sm border border-slate-100 p-4 mt-6 mb-4">
        <h2 className="text-sm font-bold text-slate-800 uppercase tracking-wider mb-4">Service Route Map</h2>
        <div className="relative w-full h-56 bg-slate-100 rounded-xl overflow-hidden border border-slate-200">
           {/* Abstract Map Grid Background */}
           <div className="absolute inset-0 opacity-40" style={{ backgroundImage: 'radial-gradient(#94a3b8 1px, transparent 1px)', backgroundSize: '24px 24px' }}></div>
           
           {/* Map Route SVG Line */}
           <svg className="absolute inset-0 w-full h-full pointer-events-none z-0">
             <path d="M 100 80 L 220 50" stroke="#3b82f6" strokeWidth="2" strokeDasharray="4 4" className="opacity-60" />
             <path d="M 220 50 L 170 160" stroke="#3b82f6" strokeWidth="2" strokeDasharray="4 4" className="opacity-60" />
           </svg>

           {/* Current Tech Location */}
           <div className="absolute top-[40%] left-[28%] flex flex-col items-center z-20">
             <div className="w-4 h-4 bg-blue-600 rounded-full border-2 border-white shadow-lg relative">
               <div className="absolute inset-0 bg-blue-600 rounded-full animate-ping opacity-75"></div>
             </div>
             <div className="bg-slate-900 text-white text-[8px] font-bold px-1.5 py-0.5 rounded mt-1 shadow-md">YOU</div>
           </div>

           {/* Scheduled Stop 1 */}
           <div className="absolute top-[20%] left-[60%] flex flex-col items-center z-10 cursor-pointer hover:scale-110 transition-transform">
             <MapPin className="text-red-500 fill-red-100 drop-shadow-md" size={26} />
             <div className="bg-white text-slate-800 text-[9px] font-bold px-1.5 py-0.5 rounded shadow-sm border border-slate-200 mt-0.5 whitespace-nowrap">
               10:00 AM • Brake Pad
             </div>
           </div>

           {/* Scheduled Stop 2 */}
           <div className="absolute top-[70%] left-[45%] flex flex-col items-center z-10 cursor-pointer hover:scale-110 transition-transform">
             <MapPin className="text-amber-500 fill-amber-100 drop-shadow-md" size={26} />
             <div className="bg-white text-slate-800 text-[9px] font-bold px-1.5 py-0.5 rounded shadow-sm border border-slate-200 mt-0.5 whitespace-nowrap">
               2:00 PM • Alternator
             </div>
           </div>
        </div>
      </div>
    </div>
  );

  const renderSchedule = () => {
    // Generate a simple hour list for Day view
    const hours = Array.from({length: 11}, (_, i) => i + 8); // 8 AM to 6 PM
    
    // Calendar math for May 2026 (Starts on a Friday, 31 days)
    const emptyStartDays = 5; 
    const daysInMonth = 31;
    const calendarGrid = Array.from({length: 35}, (_, i) => {
      const dayNum = i - emptyStartDays + 1;
      return (dayNum > 0 && dayNum <= daysInMonth) ? dayNum : null;
    });

    return (
      <div className="flex flex-col h-full bg-slate-50">
        <div className="bg-white px-4 py-3 shadow-sm border-b border-slate-200 sticky top-0 z-10">
          <div className="flex justify-between items-center mb-3">
            <h1 className="text-2xl font-bold text-slate-900">Schedule</h1>
            <div className="flex gap-2">
               <button className="p-1.5 bg-slate-100 rounded-full text-slate-600"><Filter size={18} /></button>
               <button className="p-1.5 bg-blue-600 rounded-full text-white"><Plus size={18} /></button>
            </div>
          </div>
          
          {/* View Toggles */}
          <div className="flex p-1 bg-slate-100 rounded-xl">
            {['day', 'week', 'month'].map((view) => (
              <button 
                key={view}
                onClick={() => setScheduleView(view)}
                className={`flex-1 py-1.5 text-xs font-bold uppercase rounded-lg transition-colors ${
                  scheduleView === view ? 'bg-white text-slate-900 shadow-sm' : 'text-slate-500'
                }`}
              >
                {view}
              </button>
            ))}
          </div>
        </div>

        <div className="flex-1 overflow-y-auto pb-24 relative">
          
          {/* DAY VIEW */}
          {scheduleView === 'day' && (
            <div className="p-4">
              <h2 className="text-lg font-bold text-slate-800 mb-4 flex items-center justify-between">
                <ChevronLeft size={20} className="text-slate-400" />
                Tuesday, May 26
                <ChevronRight size={20} className="text-slate-400" />
              </h2>
              <div className="relative">
                {/* Timeline background lines */}
                <div className="absolute left-16 top-0 bottom-0 w-px bg-slate-200"></div>
                
                {hours.map(hour => {
                  const ampm = hour >= 12 ? 'PM' : 'AM';
                  const displayHour = hour > 12 ? hour - 12 : hour;
                  const timeString = `${displayHour}:00 ${ampm}`;
                  
                  // Simple collision check for mockup purposes based on hour string matching
                  const events = MOCK_SCHEDULE_EVENTS.filter(e => e.date === 26 && parseInt(e.time.split(':')[0]) === displayHour && e.time.includes(ampm));

                  return (
                    <div key={hour} className="flex min-h-[60px] relative">
                      <div className="w-16 pr-4 text-right text-xs font-medium text-slate-400 pt-2 shrink-0">
                        {timeString}
                      </div>
                      <div className="flex-1 border-t border-slate-100 pt-1 pb-1 relative">
                        {events.map((evt, idx) => (
                          <div 
                            key={idx} 
                            onClick={() => {
                              if (evt.type === 'JOB') {
                                const jobData = MOCK_JOBS.find(j => j.id === evt.jobId);
                                const customer = MOCK_CUSTOMERS.find(c => c.id === jobData.customerId);
                                const vehicle = MOCK_VEHICLES.find(v => v.id === jobData.vehicleId);
                                pushView('job_detail', { job: jobData, customer, vehicle });
                              }
                            }}
                            className={`absolute w-full z-10 p-2 rounded-lg border-l-4 shadow-sm top-2 ${evt.type === 'JOB' ? 'cursor-pointer' : ''} ${
                              evt.type === 'JOB' 
                                ? 'bg-blue-50 border-blue-500' 
                                : 'bg-slate-800 border-slate-500 text-white'
                            }`}
                            style={{ minHeight: `${evt.duration * 50}px` }}
                          >
                            <div className="flex justify-between items-start">
                              <p className={`text-xs font-bold ${evt.type === 'JOB' ? 'text-slate-900' : 'text-white'}`}>
                                {evt.title}
                              </p>
                              {evt.type === 'JOB' && <Wrench size={12} className="text-blue-500" />}
                              {evt.type === 'ADMIN' && <Settings size={12} className="text-slate-400" />}
                            </div>
                            <p className={`text-[10px] mt-1 ${evt.type === 'JOB' ? 'text-slate-500' : 'text-slate-300'}`}>
                              {evt.time} ({evt.duration}h block)
                            </p>
                          </div>
                        ))}
                      </div>
                    </div>
                  );
                })}
              </div>
            </div>
          )}

          {/* WEEK VIEW */}
          {scheduleView === 'week' && (
            <div className="p-4 space-y-6">
              <h2 className="text-lg font-bold text-slate-800 mb-2 flex items-center justify-between">
                <ChevronLeft size={20} className="text-slate-400" />
                May 24 - May 30
                <ChevronRight size={20} className="text-slate-400" />
              </h2>
              
              {[26, 27, 28].map(day => {
                const dayName = day === 26 ? 'Tuesday' : day === 27 ? 'Wednesday' : 'Thursday';
                const events = MOCK_SCHEDULE_EVENTS.filter(e => e.date === day);
                return (
                  <div key={day}>
                    <div className="flex items-center mb-3">
                      <div className="w-8 h-8 rounded-full bg-slate-200 text-slate-700 flex items-center justify-center font-bold text-sm mr-3">
                        {day}
                      </div>
                      <h3 className="font-bold text-slate-800">{dayName}</h3>
                    </div>
                    <div className="space-y-3 pl-11">
                      {events.length === 0 ? (
                        <p className="text-xs text-slate-400 italic">No scheduled items</p>
                      ) : events.map(evt => (
                        <div key={evt.id} className="p-3 bg-white rounded-xl shadow-sm border border-slate-100 flex justify-between items-center">
                           <div>
                             <p className="text-sm font-bold text-slate-900">{evt.title}</p>
                             <div className="flex items-center text-[10px] font-medium text-slate-500 mt-1">
                                <Clock size={12} className="mr-1" /> {evt.time} ({evt.duration}h)
                             </div>
                           </div>
                           <span className={`text-[10px] px-2 py-1 rounded uppercase font-bold tracking-wider ${
                             evt.type === 'JOB' ? 'bg-blue-100 text-blue-700' : 'bg-slate-200 text-slate-700'
                           }`}>
                             {evt.type}
                           </span>
                        </div>
                      ))}
                    </div>
                  </div>
                );
              })}
            </div>
          )}

          {/* MONTH VIEW */}
          {scheduleView === 'month' && (
            <div className="p-4">
              <h2 className="text-lg font-bold text-slate-800 mb-4 flex items-center justify-between">
                <ChevronLeft size={20} className="text-slate-400" />
                May 2026
                <ChevronRight size={20} className="text-slate-400" />
              </h2>
              
              {/* Calendar Grid */}
              <div className="bg-white rounded-2xl shadow-sm border border-slate-100 p-2 mb-6">
                <div className="grid grid-cols-7 text-center text-xs font-bold text-slate-400 mb-2">
                  <div>S</div><div>M</div><div>T</div><div>W</div><div>T</div><div>F</div><div>S</div>
                </div>
                <div className="grid grid-cols-7 gap-1">
                  {calendarGrid.map((day, idx) => {
                    const hasJob = MOCK_SCHEDULE_EVENTS.some(e => e.date === day && e.type === 'JOB');
                    const hasAdmin = MOCK_SCHEDULE_EVENTS.some(e => e.date === day && e.type === 'ADMIN');
                    const isToday = day === 26;
                    
                    return (
                      <div key={idx} className="aspect-square flex flex-col items-center justify-center relative rounded-xl">
                        {day && (
                          <div className={`w-8 h-8 rounded-full flex items-center justify-center text-sm ${
                            isToday ? 'bg-blue-600 text-white font-bold shadow-md' : 'text-slate-700 font-medium'
                          }`}>
                            {day}
                          </div>
                        )}
                        {day && (hasJob || hasAdmin) && (
                          <div className="flex gap-1 absolute bottom-1">
                            {hasJob && <div className={`w-1 h-1 rounded-full ${isToday ? 'bg-white' : 'bg-blue-500'}`}></div>}
                            {hasAdmin && <div className={`w-1 h-1 rounded-full ${isToday ? 'bg-white opacity-70' : 'bg-slate-400'}`}></div>}
                          </div>
                        )}
                      </div>
                    );
                  })}
                </div>
              </div>

              {/* Selected Day Details */}
              <h3 className="font-bold text-slate-800 mb-3">Agenda for May 26</h3>
              <div className="space-y-3">
                 {MOCK_SCHEDULE_EVENTS.filter(e => e.date === 26).map(evt => (
                    <div key={evt.id} className="p-3 bg-white rounded-xl shadow-sm border border-slate-100 flex items-start border-l-4" style={{borderLeftColor: evt.type === 'JOB' ? '#3b82f6' : '#94a3b8'}}>
                       <div className="w-16 shrink-0 text-xs font-bold text-slate-600 pt-0.5">{evt.time}</div>
                       <div>
                         <p className="text-sm font-bold text-slate-900">{evt.title}</p>
                         <p className="text-[10px] text-slate-500 uppercase tracking-wider mt-0.5">{evt.type} BLOCK</p>
                       </div>
                    </div>
                 ))}
              </div>
            </div>
          )}

        </div>
      </div>
    );
  };

  const renderJobDetail = () => {
    const { job, customer, vehicle } = currentView.data;
    const statuses = ['SCHEDULED', 'EN_ROUTE', 'ON_SITE', 'COMPLETE'];
    const currentStatusIdx = statuses.indexOf(job.status);

    return (
      <div className="flex flex-col h-full bg-slate-50">
        <div className="bg-white px-4 py-3 shadow-sm flex items-center border-b border-slate-200 sticky top-0 z-10">
          <button onClick={popView} className="mr-3 text-slate-600 active:bg-slate-100 p-1 rounded-full">
            <ArrowLeft size={24} />
          </button>
          <div className="flex-1">
            <h2 className="font-bold text-slate-900 truncate">Job Details</h2>
            <p className="text-xs text-slate-500">ID: {job.id.toUpperCase()}</p>
          </div>
          <MoreVertical size={20} className="text-slate-600" />
        </div>

        <div className="flex-1 overflow-y-auto pb-24">
          <div className="bg-white p-4 border-b border-slate-200 mb-4">
             <div className="flex justify-between items-center relative">
                <div className="absolute left-0 top-1/2 -translate-y-1/2 w-full h-1 bg-slate-100 z-0"></div>
                {statuses.map((status, idx) => (
                  <div key={status} className="relative z-10 flex flex-col items-center group">
                    <div className={`w-8 h-8 rounded-full flex items-center justify-center text-xs font-bold shadow-sm transition-colors ${
                      idx < currentStatusIdx ? 'bg-emerald-500 text-white' :
                      idx === currentStatusIdx ? 'bg-blue-600 text-white ring-4 ring-blue-100' :
                      'bg-white text-slate-400 border-2 border-slate-200'
                    }`}>
                      {idx < currentStatusIdx ? <CheckCircle2 size={16} /> : idx + 1}
                    </div>
                  </div>
                ))}
             </div>
             <p className="text-center mt-3 text-sm font-bold text-slate-700 uppercase tracking-wider">
               Status: {job.status.replace('_', ' ')}
             </p>
          </div>

          <div className="px-4 space-y-4">
            <div className="bg-white p-4 rounded-2xl shadow-sm border border-slate-100">
              <h3 className="font-bold text-slate-900 text-lg mb-2">{job.description}</h3>
              <div className="space-y-3">
                <div className="flex items-start text-sm">
                  <MapPin size={16} className="text-slate-400 mr-3 mt-0.5" />
                  <span className="text-slate-700">{job.address}</span>
                </div>
                <div className="flex items-center text-sm">
                  <Clock size={16} className="text-slate-400 mr-3" />
                  <span className="text-slate-700">{job.scheduledStart}</span>
                </div>
              </div>
            </div>

            <div className="bg-white p-4 rounded-2xl shadow-sm border border-slate-100">
               <h4 className="text-xs font-bold text-slate-400 uppercase tracking-wider mb-3">Customer & Vehicle</h4>
               <div className="flex justify-between items-center border-b border-slate-100 pb-3 mb-3">
                 <div>
                   <p className="font-bold text-slate-900">{customer.firstName} {customer.lastName}</p>
                   <p className="text-sm text-slate-500">{customer.phone}</p>
                 </div>
                 <div className="flex gap-2">
                   <div className="w-10 h-10 bg-slate-100 rounded-full flex items-center justify-center text-slate-600">
                     <Phone size={18} />
                   </div>
                   <div className="w-10 h-10 bg-slate-100 rounded-full flex items-center justify-center text-slate-600">
                     <MessageSquare size={18} />
                   </div>
                 </div>
               </div>
               <div>
                 <p className="font-bold text-slate-900">{vehicle.year} {vehicle.make} {vehicle.model}</p>
                 <p className="text-sm text-slate-500">{vehicle.engine}</p>
               </div>
            </div>

             <div className="grid grid-cols-2 gap-3 mt-6">
                {job.status === 'SCHEDULED' && (
                  <button className="col-span-2 bg-blue-600 text-white font-bold py-3 rounded-xl flex items-center justify-center shadow-md">
                    <Navigation size={18} className="mr-2" /> Mark En Route
                  </button>
                )}
                {job.status === 'EN_ROUTE' && (
                  <button className="col-span-2 bg-amber-500 text-white font-bold py-3 rounded-xl flex items-center justify-center shadow-md">
                    <MapPin size={18} className="mr-2" /> Arrived On Site
                  </button>
                )}
                {job.status === 'ON_SITE' && (
                  <>
                    <button className="bg-emerald-600 text-white font-bold py-3 rounded-xl flex items-center justify-center shadow-md">
                      <Play size={18} className="mr-2 fill-current" /> Clock In
                    </button>
                    <button className="bg-slate-800 text-white font-bold py-3 rounded-xl flex items-center justify-center shadow-md">
                      <Receipt size={18} className="mr-2" /> Build Invoice
                    </button>
                    <button className="col-span-2 bg-slate-200 text-slate-800 font-bold py-3 rounded-xl flex items-center justify-center">
                      <CheckCircle2 size={18} className="mr-2" /> Mark Complete
                    </button>
                  </>
                )}
                {job.status === 'COMPLETE' && (
                  <button className="col-span-2 bg-slate-900 text-white font-bold py-3 rounded-xl flex items-center justify-center shadow-md">
                    <Receipt size={18} className="mr-2" /> View Invoice ({formatCents(job.invoicedCents)})
                  </button>
                )}
             </div>
          </div>
        </div>
      </div>
    );
  };

  const renderComms = () => (
    <div className="flex-1 overflow-y-auto bg-gray-50 p-4 pb-24">
      <div className="flex justify-between items-center mb-6">
        <h1 className="text-2xl font-bold text-slate-900">Messages</h1>
        <Plus className="text-slate-600" size={24} />
      </div>
      <div className="bg-blue-50 border border-blue-100 text-blue-800 text-xs p-3 rounded-xl mb-4 flex items-center">
        <span className="w-2 h-2 rounded-full bg-blue-600 mr-2 animate-pulse"></span>
        SmsReceiver Intercept Active (Offline-first)
      </div>
      <div className="space-y-2">
        {MOCK_CUSTOMERS.map(c => (
           <div key={c.id} className="bg-white p-4 rounded-xl border border-slate-100 flex items-center shadow-sm">
             <div className="w-12 h-12 rounded-full bg-slate-100 flex items-center justify-center text-slate-600 font-bold text-lg">
               {c.firstName[0]}{c.lastName[0]}
             </div>
             <div className="ml-4 flex-1">
               <div className="flex justify-between">
                 <h3 className="font-bold text-slate-900">{c.firstName} {c.lastName}</h3>
                 <span className="text-xs text-slate-400">10:42 AM</span>
               </div>
               <p className="text-sm text-slate-500 truncate">Sounds good, see you then.</p>
             </div>
           </div>
        ))}
      </div>
    </div>
  );

  const renderScreen = () => {
    switch(currentView.view) {
      case 'dashboard': return renderDashboard();
      case 'schedule': return renderSchedule();
      case 'job_detail': return renderJobDetail();
      case 'comms': return renderComms();
      default: return renderDashboard();
    }
  };

  return (
    <div className="flex h-screen w-full items-center justify-center bg-slate-900 font-sans">
      <div className="relative w-full max-w-[400px] h-full max-h-[850px] bg-black sm:rounded-[3rem] sm:border-[8px] sm:border-slate-800 overflow-hidden shadow-2xl flex flex-col">
        <div className="h-8 w-full bg-white flex justify-between items-center px-6 text-slate-900 text-xs font-medium z-50">
           <span>9:41</span>
           <div className="flex gap-1.5 items-center">
             <div className="w-4 h-4 rounded-full bg-slate-200"></div>
           </div>
        </div>

        <div className="flex-1 bg-white relative flex flex-col overflow-hidden">
          {renderScreen()}
        </div>

        {/* Updated Bottom Navigation (4 Tabs) */}
        {navStack.length === 0 && (
          <div className="absolute bottom-0 w-full bg-white border-t border-slate-200 flex justify-around pb-6 pt-3 px-2 z-50">
            <button 
              onClick={() => setCurrentTab('dashboard')}
              className={`flex flex-col items-center p-2 min-w-[72px] ${currentTab === 'dashboard' ? 'text-blue-600' : 'text-slate-500'}`}
            >
              <LayoutDashboard size={24} className={currentTab === 'dashboard' ? 'fill-blue-50' : ''} />
              <span className="text-[10px] mt-1 font-bold">Dash</span>
            </button>
            <button 
              onClick={() => setCurrentTab('schedule')}
              className={`flex flex-col items-center p-2 min-w-[72px] ${currentTab === 'schedule' ? 'text-blue-600' : 'text-slate-500'}`}
            >
              <Calendar size={24} className={currentTab === 'schedule' ? 'fill-blue-50' : ''} />
              <span className="text-[10px] mt-1 font-bold">Schedule</span>
            </button>
            <button 
              onClick={() => setCurrentTab('comms')}
              className={`flex flex-col items-center p-2 min-w-[72px] ${currentTab === 'comms' ? 'text-blue-600' : 'text-slate-500'}`}
            >
              <MessageSquare size={24} className={currentTab === 'comms' ? 'fill-blue-50' : ''} />
              <span className="text-[10px] mt-1 font-bold">Comms</span>
            </button>
            <button className="flex flex-col items-center p-2 min-w-[72px] text-slate-500">
              <Menu size={24} />
              <span className="text-[10px] mt-1 font-bold">More</span>
            </button>
          </div>
        )}
      </div>
    </div>
  );
}