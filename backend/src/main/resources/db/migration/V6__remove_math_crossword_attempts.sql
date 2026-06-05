delete from quiz_attempts
where mode = 'MATH_CROSSWORD'
   or included_modes_json like '%MATH_CROSSWORD%'
   or questions_json like '%MATH_CROSSWORD%'
   or questions_json like '%CROSSWORD%';
