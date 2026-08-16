const COURSES = [
  {
    id: "daa",
    name: "Design and Analysis of Algorithms",
    shortName: "Design & Analysis of Algorithms",
    code: "ECS 5101 / MCA20-303",
    type: "regular",
    classUrl: "https://teams.microsoft.com/meet/49556966249648?p=iaomzmLoL9QjBCW4As"
  },
  {
    id: "fcs",
    name: "Foundations of Computer Systems",
    shortName: "Foundations of Computer Systems",
    code: "ECS 5102",
    type: "regular",
    classUrl: "https://teams.microsoft.com/meet/45942092932647?p=qVjJ9lSZW9zq9hWW6S"
  },
  {
    id: "twss",
    name: "Technical Writing and Soft Skill",
    shortName: "Technical Writing & Soft Skill",
    code: "EHS 5104",
    type: "regular",
    classUrl: "https://teams.microsoft.com/meet/428254824983011?p=b3APHhyFDPb17JVMfJ"
  },
  {
    id: "ps",
    name: "Probability and Statistics",
    shortName: "Probability & Statistics",
    code: "EMC 5103",
    type: "regular",
    sessionLinks: [
      { label: "Mon 6–8 PM", url: "https://teams.microsoft.com/meet/495302915846604?p=hmQEpqiBI2uSnbkNND", lab: false },
      { label: "Wed/Thu 7–8:30 PM", url: "https://teams.microsoft.com/meet/479816469966136?p=x3vTS8Q3COOiL3uaoc", lab: true }
    ]
  },
  {
    id: "cda",
    name: "Computational Data Analysis",
    shortName: "Computational Data Analysis",
    code: "EAI 6101 / ECS 6102",
    type: "elective",
    classUrl: "https://teams.microsoft.com/meet/473767059444565?p=35U3s0oHmC0CczOeKL"
  },
  {
    id: "pr",
    name: "Pattern Recognition",
    shortName: "Pattern Recognition",
    code: "EAI 6102 / ECS 6303 / ESD 6102 / EAS 6102 / MCA20-E305F",
    type: "elective",
    sessionLinks: [
      { label: "Sun 9:30–11 AM", url: "https://teams.microsoft.com/meet/482132438614705?p=NNifVQwkXJ2ZI0BOjD", lab: false },
      { label: "Sun 5–6:30 PM", url: "https://teams.microsoft.com/meet/441818680909696?p=taHUzSmA9ZdMcG0j5X", lab: true }
    ]
  },
  {
    id: "aml",
    name: "Advanced Machine Learning",
    shortName: "Advanced Machine Learning",
    code: "EAI 6103 / EAS 6103",
    type: "elective",
    classUrl: "https://teams.microsoft.com/meet/417655912946994?p=2akmKtvzvvLw5iUzkG"
  }
];

const SCHEDULE = [
  { day:"Monday", time:"6:00 PM – 8:00 PM", course:"ps", lab:false },

  { day:"Tuesday", time:"5:00 PM – 6:30 PM", course:"twss", lab:false },
  { day:"Tuesday", time:"7:30 PM – 9:00 PM", course:"aml", lab:false },

  { day:"Wednesday", time:"5:00 PM – 6:30 PM", course:"twss", lab:false },
  { day:"Wednesday", time:"7:00 PM – 8:30 PM", course:"ps", lab:true },

  { day:"Thursday", time:"7:00 PM – 8:30 PM", course:"ps", lab:true },

  { day:"Saturday", time:"8:00 AM – 9:30 AM", course:"daa", lab:false },
  { day:"Saturday", time:"9:30 AM – 11:00 AM", course:"elective-slot", lab:false },
  { day:"Saturday", time:"11:30 AM – 1:00 PM", course:"cda", lab:false },
  { day:"Saturday", time:"7:30 PM – 9:30 PM", course:"fcs", lab:false },

  { day:"Sunday", time:"8:00 AM – 9:30 AM", course:"daa", lab:false },
  { day:"Sunday", time:"9:30 AM – 11:00 AM", course:"pr", lab:false },
  { day:"Sunday", time:"11:00 AM – 1:00 PM", course:"daa", lab:false, showLabTag:true },
  { day:"Sunday", time:"2:00 PM – 5:00 PM", course:"fcs", lab:false },
  { day:"Sunday", time:"5:00 PM – 6:30 PM", course:"pr", lab:true }
];

const DAYS = ["Monday", "Tuesday", "Wednesday", "Thursday", "Friday", "Saturday", "Sunday"];

const TIMES = [
  "8:00 AM – 9:30 AM",
  "9:30 AM – 11:00 AM",
  "11:00 AM – 1:00 PM",
  "11:30 AM – 1:00 PM",
  "2:00 PM – 5:00 PM",
  "5:00 PM – 6:30 PM",
  "6:00 PM – 8:00 PM",
  "7:00 PM – 8:30 PM",
  "7:30 PM – 9:00 PM",
  "7:30 PM – 9:30 PM"
];
